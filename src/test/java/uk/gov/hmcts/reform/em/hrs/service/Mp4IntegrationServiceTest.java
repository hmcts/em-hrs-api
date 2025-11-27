package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobDownloadContentResponse;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mp4MimeTypeService")
class Mp4MimeTypeServiceTest {

    private Mp4MimeTypeService mp4MimeTypeService;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @BeforeEach
    void setUp() {
        mp4MimeTypeService = new Mp4MimeTypeService();
    }

    @Nested
    @DisplayName("Happy path classification with real resources")
    class RealFileClassificationTests {

        @DisplayName("Should correctly identify MIME types from file structure")
        @ParameterizedTest(name = "{index} => File=''{0}'', Expected=''{1}''")
        @CsvSource({
            "classpath:video_sample.mp4, video/mp4",
            "classpath:audio_sample.mp4, audio/mp4",
            "classpath:invalid_file.txt, application/octet-stream"
        })
        void shouldDetectMimeTypesFromResources(String resourcePath, String expectedMimeType) throws IOException {
            setupMockForFile(resourcePath);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo(expectedMimeType);
        }

        @Test
        @DisplayName("Audio-only MP4 must not be misclassified as video/mp4")
        void shouldNotMisclassifyAudioOnlyAsVideo() throws IOException {
            setupMockForFile("classpath:audio_sample.mp4");

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result)
                .as("Audio-only MP4 should be classified as audio/mp4")
                .isEqualTo("audio/mp4");
        }
    }

    @Nested
    @DisplayName("Error and fallback behaviour")
    class ErrorAndFallbackTests {

        @Test
        @DisplayName("Should fallback to application/octet-stream when BlobClient throws")
        void shouldDefaultToUnknownWhenBlobClientThrows() {
            when(blobClient.getProperties()).thenThrow(new RuntimeException("Azure Down"));

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should fallback to application/octet-stream for file smaller than minimal atom header")
        void shouldFallbackForTooSmallFile() {
            byte[] data = new byte[] {0, 0, 0, 16};
            setupMockForBytes(data, 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should fallback to application/octet-stream when large-size atom header is incomplete")
        void shouldFallbackForIncompleteLargeSizeHeader() {
            ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(1);               // size == 1 (large size)
            buffer.put("ftyp".getBytes());  // type
            buffer.putInt(0);               // not enough bytes for 8-byte largesize

            setupMockForBytes(buffer.array(), 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should handle size==0 atom header extending to EOF and classify as unknown")
        void shouldHandleZeroSizeAtomExtendingToEof() {
            ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(0);               // size == 0 (extends to EOF)
            buffer.put("ftyp".getBytes());  // type
            buffer.putInt(0x01020304);      // payload
            buffer.putInt(0x05060708);      // payload

            setupMockForBytes(buffer.array(), 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should stop scanning when encountering malformed atom size and classify as unknown")
        void shouldStopScanningOnMalformedAtomSize() {
            ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);

            buffer.putInt(8);               // ftyp size
            buffer.put("ftyp".getBytes());  // type

            buffer.putInt(32);              // malformed atom that claims size 32 (beyond EOF)
            buffer.put("xxxx".getBytes());  // type

            buffer.putInt(0x01020304);
            buffer.putInt(0x05060708);

            setupMockForBytes(buffer.array(), 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should return application/octet-stream for truncated MP4")
        void shouldFallbackForTruncatedMp4() throws IOException {
            File file = ResourceUtils.getFile("classpath:video_sample.mp4");
            long realSize = Files.size(file.toPath());

            long truncatedSize = Math.min(realSize / 4, 1024);

            lenient().when(blobClient.getProperties()).thenReturn(blobProperties);
            lenient().when(blobProperties.getBlobSize()).thenReturn(truncatedSize);

            lenient().when(blobClient.downloadContentWithResponse(
                any(DownloadRetryOptions.class),
                isNull(),
                any(BlobRange.class),
                anyBoolean(),
                isNull(),
                any(Context.class))
            ).thenAnswer(invocation -> {
                BlobRange range = invocation.getArgument(2);
                long requestedOffset = range.getOffset();
                Long requestedCount = range.getCount();

                long maxReadable = Math.max(0, truncatedSize - requestedOffset);
                long count = Objects.isNull(requestedCount)
                    ? Math.min(1024L, maxReadable)
                    : Math.min(requestedCount, maxReadable);

                byte[] fileBytes = readBytesFromLocalFile(file, requestedOffset, count);

                BlobDownloadContentResponse response = mock(BlobDownloadContentResponse.class);
                when(response.getValue()).thenReturn(BinaryData.fromBytes(fileBytes));
                return response;
            });

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }
    }

    @Nested
    @DisplayName("Targeted internal behaviour coverage")
    class InternalBehaviourTests {

        @Test
        @DisplayName("Should parse large-size atom header (size == 1) without throwing")
        void shouldParseLargeSizeAtomHeader() {
            ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);

            buffer.putInt(1);                         // size == 1 (large)
            buffer.put("ftyp".getBytes());            // type
            buffer.putLong(32L);                      // largesize
            buffer.putInt(0);                         // payload (ignored)
            buffer.putInt(0);                         // payload (ignored)

            setupMockForBytes(buffer.array(), 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should return early from processHandlerAtom when payload offset is out of bounds")
        void shouldReturnEarlyWhenHandlerPayloadOutOfBounds() {
            ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);

            int moovSize = 64;
            buffer.putInt(moovSize);
            buffer.put("moov".getBytes());

            int hdlrSize = 12;
            buffer.putInt(hdlrSize);
            buffer.put("hdlr".getBytes());

            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 0);

            buffer.putInt(0);

            while (buffer.position() < 64) {
                buffer.put((byte) 0);
            }

            setupMockForBytes(buffer.array(), 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should bypass cache in CachedBlobReader when requested length exceeds buffer size")
        void shouldBypassCacheWhenLengthExceedsBufferSize() {
            byte[] data = new byte[8192];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i & 0xFF);
            }

            int smallBufferSize = 1024;

            AtomicReference<BlobRange> lastRange = new AtomicReference<>();

            long fileSize = data.length;

            lenient().when(blobClient.getProperties()).thenReturn(blobProperties);
            lenient().when(blobProperties.getBlobSize()).thenReturn(fileSize);

            lenient().when(blobClient.downloadContentWithResponse(
                any(DownloadRetryOptions.class),
                isNull(),
                any(BlobRange.class),
                anyBoolean(),
                isNull(),
                any(Context.class))
            ).thenAnswer(invocation -> {
                BlobRange range = invocation.getArgument(2);
                lastRange.set(range);

                long offset = range.getOffset();
                Long count = range.getCount();

                long maxReadable = Math.max(0, fileSize - offset);
                long length = Objects.isNull(count)
                    ? Math.min(1024L, maxReadable)
                    : Math.min(count, maxReadable);

                int len = (int) length;
                byte[] slice = new byte[len];

                if (len > 0 && offset < fileSize) {
                    System.arraycopy(data, (int) offset, slice, 0, len);
                }

                BlobDownloadContentResponse response = mock(BlobDownloadContentResponse.class);
                when(response.getValue()).thenReturn(BinaryData.fromBytes(slice));
                return response;
            });

            Mp4MimeTypeService.CachedBlobReader reader =
                new Mp4MimeTypeService.CachedBlobReader(blobClient, fileSize, smallBufferSize);

            byte[] result = reader.read(0, smallBufferSize * 2);

            assertThat(result).hasSize(smallBufferSize * 2);
            assertThat(lastRange.get()).isNotNull();
            assertThat(lastRange.get().getOffset()).isZero();
            assertThat(lastRange.get().getCount()).isEqualTo((long) smallBufferSize * 2);
        }

        @Test
        @DisplayName("Should return early from processHandlerAtom when handler_type is out of bounds")
        void shouldReturnEarlyWhenHandlerTypeOutOfBounds() {
            byte[] data = buildMoovWithTooSmallHdlr();

            // We do not care about range capture here, only behaviour
            setupMockForBytes(data, 4096, null);

            String result = mp4MimeTypeService.getMimeType(blobClient);

            assertThat(result).isEqualTo("application/octet-stream");
        }
    }

    private void setupMockForFile(String resourcePath) throws IOException {
        File file = ResourceUtils.getFile(resourcePath);
        long fileSize = Files.size(file.toPath());

        lenient().when(blobClient.getProperties()).thenReturn(blobProperties);
        lenient().when(blobProperties.getBlobSize()).thenReturn(fileSize);

        lenient().when(blobClient.downloadContentWithResponse(
            any(DownloadRetryOptions.class),
            isNull(),
            any(BlobRange.class),
            anyBoolean(),
            isNull(),
            any(Context.class))
        ).thenAnswer(invocation -> {
            BlobRange range = invocation.getArgument(2);
            long offset = range.getOffset();
            Long count = range.getCount();

            byte[] fileBytes = readBytesFromLocalFile(file, offset, count);

            BlobDownloadContentResponse response = mock(BlobDownloadContentResponse.class);
            when(response.getValue()).thenReturn(BinaryData.fromBytes(fileBytes));
            return response;
        });
    }

    private void setupMockForBytes(byte[] data, int bufferSize, AtomicReference<BlobRange> captureRange) {
        long fileSize = data.length;

        lenient().when(blobClient.getProperties()).thenReturn(blobProperties);
        lenient().when(blobProperties.getBlobSize()).thenReturn(fileSize);

        lenient().when(blobClient.downloadContentWithResponse(
            any(DownloadRetryOptions.class),
            isNull(),
            any(BlobRange.class),
            anyBoolean(),
            isNull(),
            any(Context.class))
        ).thenAnswer(invocation -> {
            BlobRange range = invocation.getArgument(2);
            if (Objects.nonNull(captureRange)) {
                captureRange.set(range);
            }

            long offset = range.getOffset();
            Long count = range.getCount();

            long maxReadable = Math.max(0, fileSize - offset);
            long length = Objects.isNull(count)
                ? Math.min(bufferSize, maxReadable)
                : Math.min(count, maxReadable);

            int len = (int) length;
            byte[] slice = new byte[len];

            if (len > 0 && offset < fileSize) {
                System.arraycopy(data, (int) offset, slice, 0, len);
            }

            BlobDownloadContentResponse response = mock(BlobDownloadContentResponse.class);
            when(response.getValue()).thenReturn(BinaryData.fromBytes(slice));
            return response;
        });
    }

    /**
     * Build a minimal fake MP4-like structure with:
     * - ftyp at 0
     * - moov container
     * - a single hdlr atom whose size is too small for the handler_type field.
     *
     * Used to exercise processHandlerAtom's out-of-bounds early return.
     */
    private byte[] buildMoovWithTooSmallHdlr() {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN);

        // ftyp (8 bytes, no payload)
        buffer.putInt(8);                       // size
        buffer.put("ftyp".getBytes());         // type

        // moov container: covers the rest of the file
        int moovSize = 56;                     // from current pos (8) to end (64)
        buffer.putInt(moovSize);               // size
        buffer.put("moov".getBytes());         // type

        // hdlr atom inside moov
        // headerLen = 8, HANDLER_PAYLOAD_SKIP = 8, HANDLER_TYPE_LENGTH = 4
        // payloadOffset = cursor + 8 + 8 = cursor + 16
        // We declare size = 12 so cursor + size = cursor + 12
        // => payloadOffset + 4 > cursor + 12, triggers early-return branch.
        int hdlrSize = 12;
        buffer.putInt(hdlrSize);               // size
        buffer.put("hdlr".getBytes());         // type

        // version(1) + flags(3) + pre_defined(4) = 8 bytes total
        buffer.put((byte) 0);                  // version
        buffer.put((byte) 0);                  // flags[0]
        buffer.put((byte) 0);                  // flags[1]
        buffer.put((byte) 0);                  // flags[2]
        buffer.putInt(0);                      // pre_defined

        while (buffer.position() < buffer.capacity()) {
            buffer.put((byte) 0);
        }

        return buffer.array();
    }

    private byte[] readBytesFromLocalFile(File file, long offset, Long count) throws IOException {
        long fileSize = Files.size(file.toPath());
        long maxReadable = Math.max(0, fileSize - offset);

        long effectiveCount = Objects.isNull(count)
            ? Math.min(1024L, maxReadable)
            : Math.min(count, maxReadable);

        int readLength = (int) effectiveCount;
        byte[] buffer = new byte[readLength];

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            if (readLength > 0) {
                raf.readFully(buffer, 0, readLength);
            }
        }

        return buffer;
    }
}
