package chat.util;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片处理工具类 (使用Thumbnailator库)
 */
public final class ImageUtil {

    private static final int AVATAR_SIZE = 128;

    private ImageUtil() {}

    /**
     * 将输入的图片流缩放为标准头像尺寸。
     * @param inputStream 原始图片的输入流
     * @return 缩放后的PNG格式图片的字节数组
     * @throws IOException 如果图片处理失败
     */
    public static byte[] resizeAvatar(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(inputStream)
                    .size(AVATAR_SIZE, AVATAR_SIZE)
                    .outputFormat("png") // 统一输出为PNG
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }
}