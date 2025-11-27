package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Service
public class Mp4MimeTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(Mp4MimeTypeService.class);

    private static final String VIDEO_MIME = "video/mp4";
    private static final String AUDIO_MIME = "audio/mp4";
    private static final String UNKNOWN_MIME = "application/octet-stream";

    private static final String ATOM_FTYP = "ftyp";
    private static final String ATOM_MOOV = "moov";
    private static final String ATOM_TRAK = "trak";
    private static final String ATOM_MDIA = "mdia";
    private static final String ATOM_HDLR = "hdlr";

    private static final int MIN_ATOM_SIZE = 8;
    private static final int EXTENDED_HEADER_SIZE = 16;
    private static final int TYPE_FIELD_LENGTH = 4;

    private static final int HANDLER_PAYLOAD_SKIP = 8;
    private static final int HANDLER_TYPE_LENGTH = 4;

    private static final int READ_BUFFER_SIZE = 4096;

    public String getMimeType(BlobClient blobClient) {
        try {
            long fileSize = blobClient.getProperties().getBlobSize();
            CachedBlobReader reader = new CachedBlobReader(blobClient, fileSize, READ_BUFFER_SIZE);

            if (!isValidMp4(reader)) {
                LOG.warn("Blob {} is not a valid MP4 (missing ftyp).", blobClient.getBlobName());
                return UNKNOWN_MIME;
            }

            TrackFlags flags = inspectTracks(reader, 0, fileSize, TrackFlags.empty());

            if (flags.hasVideo()) {
                return VIDEO_MIME;
            }

            if (flags.hasAudio()) {
                return AUDIO_MIME;
            }

            return UNKNOWN_MIME;

        } catch (Exception e) {
            LOG.error("Error parsing MP4 structure for Blob: {}. Defaulting to Unknown.", blobClient.getBlobName(), e);
            return UNKNOWN_MIME;
        }
    }

    private boolean isValidMp4(CachedBlobReader reader) throws IOException {
        AtomHeader header = parseAtomHeader(reader, 0);

        if (Objects.isNull(header)) {
            return false;
        }

        return ATOM_FTYP.equals(header.type());
    }

    /**
     * Recursively walks atoms in [startPos, endPos), updating track flags.
     */
    private TrackFlags inspectTracks(
        CachedBlobReader reader,
        long startPos,
        long endPos,
        TrackFlags flags
    ) throws IOException {

        long cursor = startPos;

        while (cursor < endPos) {
            if (flags.hasVideo()) {
                return flags;
            }

            if (cursor + MIN_ATOM_SIZE > endPos) {
                return flags;
            }

            AtomHeader atom = parseAtomHeader(reader, cursor);

            if (Objects.isNull(atom)) {
                LOG.debug("Null atom detected at {}. Stopping scan.", cursor);
                return flags;
            }

            long atomEnd = cursor + atom.size();

            if (atom.size() < MIN_ATOM_SIZE || atomEnd > endPos) {
                LOG.debug("Invalid or OOB atom detected at {}. Stopping scan.", cursor);
                return flags;
            }

            String type = atom.type();
            long childStart = cursor + atom.headerLen();

            switch (type) {
                case ATOM_HDLR -> flags = processHandlerAtom(reader, cursor, atom, flags);
                case ATOM_MOOV, ATOM_TRAK, ATOM_MDIA -> flags = inspectTracks(reader, childStart, atomEnd, flags);
                default -> {
                    // ignore
                }
            }

            cursor += atom.size();
        }

        return flags;
    }

    private AtomHeader parseAtomHeader(CachedBlobReader reader, long cursor) throws IOException {
        byte[] headerBytes = reader.read(cursor, EXTENDED_HEADER_SIZE);

        if (headerBytes.length < MIN_ATOM_SIZE) {
            return null;
        }

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(headerBytes))) {
            long size = Integer.toUnsignedLong(dis.readInt());
            String type = new String(dis.readNBytes(TYPE_FIELD_LENGTH), StandardCharsets.US_ASCII);
            int headerLen = MIN_ATOM_SIZE;

            if (size == 1) {
                if (headerBytes.length < EXTENDED_HEADER_SIZE) {
                    return null;
                }

                size = dis.readLong();
                headerLen = EXTENDED_HEADER_SIZE;
            } else if (size == 0) {
                size = reader.fileSize() - cursor;
            }

            return new AtomHeader(size, type, headerLen);
        }
    }

    private TrackFlags processHandlerAtom(
        CachedBlobReader reader,
        long cursor,
        AtomHeader atom,
        TrackFlags flags
    ) {

        long payloadOffset = cursor + atom.headerLen() + HANDLER_PAYLOAD_SKIP;
        long atomEnd = cursor + atom.size();

        if (payloadOffset + HANDLER_TYPE_LENGTH > atomEnd) {
            return flags;
        }

        byte[] hdlrData = reader.read(payloadOffset, HANDLER_TYPE_LENGTH);

        if (hdlrData.length < HANDLER_TYPE_LENGTH) {
            return flags;
        }

        String subType = new String(hdlrData, StandardCharsets.US_ASCII);

        if ("vide".equals(subType)) {
            return flags.withVideo();
        }

        if ("soun".equals(subType)) {
            return flags.withAudio();
        }

        return flags;
    }

    private record AtomHeader(long size, String type, int headerLen) { }

    /**
     * Simple immutable value object for track presence flags.
     */
    private record TrackFlags(boolean hasVideo, boolean hasAudio) {

        static TrackFlags empty() {
            return new TrackFlags(false, false);
        }

        TrackFlags withVideo() {
            return new TrackFlags(true, hasAudio);
        }

        TrackFlags withAudio() {
            return new TrackFlags(hasVideo, true);
        }
    }

    static final class CachedBlobReader {

        private final BlobClient client;
        private final long fileSize;
        private final int bufferSize;

        private byte[] buffer;
        private long bufferStartOffset = -1;

        CachedBlobReader(BlobClient client, long fileSize, int bufferSize) {
            this.client = client;
            this.fileSize = fileSize;
            this.bufferSize = bufferSize;
        }

        long fileSize() {
            return fileSize;
        }

        byte[] read(long offset, int length) {
            if (length > bufferSize) {
                return fetchFromNetwork(offset, length);
            }

            if (Objects.nonNull(buffer)
                && offset >= bufferStartOffset
                && offset + length <= bufferStartOffset + buffer.length) {

                int relativeStart = (int) (offset - bufferStartOffset);
                return Arrays.copyOfRange(buffer, relativeStart, relativeStart + length);
            }

            long remaining = Math.max(0, fileSize - offset);
            long fetchSize = Math.min(bufferSize, remaining);

            buffer = fetchFromNetwork(offset, (int) fetchSize);
            bufferStartOffset = offset;

            int available = Math.min(length, buffer.length);
            return Arrays.copyOfRange(buffer, 0, available);
        }

        private byte[] fetchFromNetwork(long offset, int length) {
            BlobRange range = new BlobRange(offset, (long) length);
            DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(3);

            return client
                .downloadContentWithResponse(options, null, range, false, null, Context.NONE)
                .getValue()
                .toBytes();
        }
    }
}
