package pyguy;

import net.sourceforge.tess4j.*;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import pyguy.types.ScanResult;

import java.awt.image.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class AchievementScanner
{
    private static ExecutorService threadPool = null;

    private static boolean cancelled = false;

    private static class ThreadContext
    {
        // Process
        List<ScanResult> processOutput = new ArrayList<>();

        Mat areaMat    = new Mat();
        Mat processMat = new Mat();
        Mat approxMat  = new Mat();
        Mat dummy      = new Mat();

        MatVector processContours = new MatVector();

        // Text
        Mat textMat = new Mat();

        // Stars
        Mat starsMat = new Mat();
        Mat hullMat  = new Mat();

        MatVector starContours = new MatVector();

        // Conversion
        BufferedImage bufferedImage = null;
    }

    private static final ThreadLocal<Tesseract> TESSERACT =
        ThreadLocal.withInitial(() -> {
            Tesseract t = new Tesseract();
            t.setDatapath("tessdata");
            t.setLanguage("eng+spa+ita+deu");
            t.setVariable("tessedit_char_blacklist", "“”‘’");
            t.setVariable("user_defined_dpi", "96");
            return t;
        });

    private static final OpenCVFrameConverter.ToMat FRAME_CONVERTER = new OpenCVFrameConverter.ToMat();

    private static final Size EMPTY_SIZE    = new Size();
    private static final Size SIZE3x3       = new Size(3, 3);
    private static final Size SIZE9x9       = new Size(9, 9);
    private static final Size SIZE1920x1080 = new Size(1920, 1080);

    private static final Rect ACHIEVEMENT_RANGE = new Rect(736, 144, 1824 - 736, 960 - 144);

    private static final Mat BLUR_KERNEL = getStructuringElement(MORPH_RECT, SIZE3x3);

    private static final ThreadLocal<ThreadContext> THREAD_CONTEXT =
        ThreadLocal.withInitial(ThreadContext::new);

    public record VideoValidity(boolean valid, String reason) {}

    public static VideoValidity IsVideoValid(File file)
    {
        try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file))
        {
            frameGrabber.start();

            if (frameGrabber.getImageWidth() <= 0 || frameGrabber.getImageHeight() <= 0)
                return new VideoValidity(false, I18N.GetString("not-a-video"));

            if (Math.abs(((double) frameGrabber.getImageWidth() / frameGrabber.getImageHeight()) - (16.0 / 9.0)) > 0.01)
                return new VideoValidity(false, I18N.GetString("wrong-aspect-ratio"));

            boolean valid = false;
            Frame frame;
            while ((frame = frameGrabber.grabImage()) != null)
            {
                if (frame.image != null)
                {
                    valid = true;
                    break;
                }
            }

            if (!valid)
                return new VideoValidity(false, I18N.GetString("no-image-data"));

            if (frameGrabber.getLengthInFrames() <= 1)
                return new VideoValidity(false, I18N.GetString("file-is-image"));

            boolean isSizeSimilar =
                !(Math.abs(frameGrabber.getImageWidth()  - 1920) > (1920 * 0.33) ||
                  Math.abs(frameGrabber.getImageHeight() - 1080) > (1080 * 0.33));

            frameGrabber.stop();

            return new VideoValidity(true, isSizeSimilar ? null : I18N.GetString("size-too-different"));
        }
        catch (Exception e)
        {
            return new VideoValidity(false, I18N.GetString("cannot-decode"));
        }
    }

    public static List<ScanResult> ProcessVideo(File video)
    {
        return ProcessVideo(video, (a, b) -> {});
    }

    public static List<ScanResult> ProcessVideo(File video, BiConsumer<Integer, Integer> progressCallback)
    {
        if (threadPool != null || cancelled)
            return null;

        ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> frameResults = new ConcurrentHashMap<>();
        List<ScanResult>                                     output       = new ArrayList<>();

        int maxCores = Runtime.getRuntime().availableProcessors();
        int cores = Settings.GetMaxThreads() == -1 ? maxCores : Math.min(maxCores, Settings.GetMaxThreads());

        try (FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(video))
        {
            threadPool = new ThreadPoolExecutor(
                cores,
                cores,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );

            frameGrabber.start();

            AtomicInteger frameCounter = new AtomicInteger(0);
            AtomicInteger frameCount   = new AtomicInteger(0);

            while (frameGrabber.grabImage() != null)
            {
                frameCount.incrementAndGet();
            }

            frameGrabber.restart();

            Frame frame;
            while ((frame = frameGrabber.grabImage()) != null)
            {
                Mat imageMat = FRAME_CONVERTER.convert(frame).clone();

                if (threadPool == null || threadPool.isShutdown())
                    break;

                threadPool.submit(() -> {
                    List<ScanResult> results = ProcessImage(imageMat);

                    for (ScanResult result : results)
                    {
                        frameResults
                            .computeIfAbsent(result.name(), k -> new ConcurrentLinkedQueue<>())
                            .add(result.stars());
                    }

                    if (!cancelled)
                        progressCallback.accept(frameCounter.incrementAndGet(), frameCount.get());
                });
            }

            if (threadPool != null)
            {
                if (!threadPool.isShutdown())
                    threadPool.shutdown();

                if (!threadPool.awaitTermination(45, TimeUnit.SECONDS))
                    System.err.println("Timeout awaiting termination of working threads.");
            }

            CleanupResources();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        for (String title : frameResults.keySet())
        {
            ConcurrentLinkedQueue<Integer> stars = frameResults.get(title);
            List<Integer> starsList = new ArrayList<>(stars);

            starsList.sort(Integer::compareTo);

            int starCount = 0;
            if (stars.size() > 2)
            {
                double avg = 0;

                for (int i = 1; i < stars.size() - 1; i++)
                {
                    avg += starsList.get(i);
                }

                avg /= (stars.size() - 2);

                starCount = (int) Math.round(avg);
            }
            else
            {
                starCount = starsList.getFirst();
            }

            output.add(new ScanResult(title, starCount));
        }

        threadPool = null;

        return output;
    }

    public static void Interrupt()
    {
        if (threadPool == null)
            return;

        cancelled = true;

        threadPool.shutdownNow();

        try
        {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS))
                System.err.println("Timeout awaiting termination of working threads.");

        } catch (Exception ignored) {}

        threadPool = null;
        cancelled = false;
    }

    public static List<ScanResult> ProcessImage(Mat img)
    {
        ThreadContext ctx = THREAD_CONTEXT.get();

        ctx.processOutput.clear();

        CheckInterrupted();

        resize(img, ctx.areaMat, SIZE1920x1080, 0, 0, INTER_CUBIC);
        Mat areaView = ctx.areaMat.apply(ACHIEVEMENT_RANGE);

        cvtColor(areaView, ctx.processMat, COLOR_BGR2GRAY);

        blur(ctx.processMat, ctx.processMat, SIZE3x3);

        Canny(ctx.processMat, ctx.processMat, 30, 70);

        ctx.processContours.clear();

        CheckInterrupted();

        findContours(
            ctx.processMat,
            ctx.processContours,
            ctx.dummy,
            RETR_EXTERNAL,
            CHAIN_APPROX_SIMPLE
        );

        int size = (int) ctx.processContours.size();
        for (int i = 0; i < size; i++)
        {
            Mat contour = ctx.processContours.get(i);

            double perimeter = arcLength(contour, true);

            approxPolyDP(
                contour,
                ctx.approxMat,
                0.02 * perimeter,
                true
            );

            if (ctx.approxMat.total() == 4)
            {
                Rect rect = boundingRect(ctx.approxMat);

                if (Math.abs(rect.area() - 132000f) > 3000f) continue;

                Mat slice = areaView.apply(rect);

                Mat starsArea = slice.apply(
                    new Rect(
                        0,
                        0,
                        (int) (slice.cols() * 0.12f),
                        slice.rows()
                    )
                );

                int textX1 = (int) (slice.cols() * 0.12f);
                int textX2 = (int) (slice.cols() * 0.76f);
                Mat textArea = slice.apply(
                    new Rect(
                        textX1,
                        0,
                        textX2 - textX1,
                        slice.rows()
                    )
                );

                CheckInterrupted();

                ctx.processOutput.add(new ScanResult(ExtractText(textArea), CountStars(starsArea)));
            }
        }

        return ctx.processOutput;
    }

    private static String ExtractText(Mat img)
    {
        ThreadContext ctx = THREAD_CONTEXT.get();

        cvtColor(img, ctx.textMat, COLOR_BGR2GRAY);

        resize(ctx.textMat, ctx.textMat, EMPTY_SIZE, 2, 2, INTER_CUBIC);

        GaussianBlur(ctx.textMat, ctx.textMat, SIZE3x3, 0);

        threshold(ctx.textMat, ctx.textMat, 0, 255, THRESH_BINARY + THRESH_OTSU);

        CheckInterrupted();

        try
        {
            return TESSERACT.get().doOCR(MatToBufferedImage(ctx.textMat)).split("\n")[0];
        }
        catch (TesseractException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    private static int CountStars(Mat img)
    {
        ThreadContext ctx = THREAD_CONTEXT.get();

        extractChannel(img, ctx.starsMat, 0);

        resize(ctx.starsMat, ctx.starsMat, EMPTY_SIZE, 2, 2, INTER_CUBIC);

        GaussianBlur(ctx.starsMat, ctx.starsMat, SIZE9x9, 0);

        threshold(ctx.starsMat, ctx.starsMat, 200, 255, THRESH_BINARY | THRESH_OTSU);

        Canny(ctx.starsMat, ctx.starsMat, 30, 70);

        morphologyEx(ctx.starsMat, ctx.starsMat, MORPH_CLOSE, BLUR_KERNEL);
        Mat starsView = ctx.starsMat.apply(
            new Rect(
                0,
                0,
                ctx.starsMat.cols(),
                (int)(ctx.starsMat.rows() * 0.75)
            )
        );

        ctx.starContours.clear();

        CheckInterrupted();

        findContours(
            starsView,
            ctx.starContours,
            ctx.dummy,
            RETR_EXTERNAL,
            CHAIN_APPROX_SIMPLE
        );

        int stars = 0;
        int size = (int) ctx.starContours.size();
        for (int i = 0; i < size; i++)
        {
            Mat contour = ctx.starContours.get(i);

            double area = contourArea(contour);

            if (area <= 0)
                continue;

            convexHull(contour, ctx.hullMat, true, true);

            double hullArea = contourArea(ctx.hullMat);

            double solidity = area / hullArea;

            Rect r = boundingRect(contour);

            double aspect = (double) r.width() / r.height();

            boolean isStar = solidity < 0.95 && (aspect > 0.7 && aspect < 1.3) && area > 400 && area < 2000;

            if (isStar) stars++;
        }

        return stars;
    }

    private static BufferedImage MatToBufferedImage(Mat mat)
    {
        ThreadContext ctx = THREAD_CONTEXT.get();

        int type = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;

        if (ctx.bufferedImage == null ||
            ctx.bufferedImage.getWidth() != mat.cols() ||
            ctx.bufferedImage.getHeight() != mat.rows())
        {
            ctx.bufferedImage = new BufferedImage(mat.cols(), mat.rows(), type);
        }

        byte[] target = ((DataBufferByte) ctx.bufferedImage.getRaster().getDataBuffer()).getData();

        mat.data().get(target);

        return ctx.bufferedImage;
    }

    private static void CheckInterrupted()
    {
        if (cancelled || Thread.currentThread().isInterrupted())
            throw new CancellationException();
    }

    private static void CleanupResources()
    {
        TESSERACT.remove();
        THREAD_CONTEXT.remove();
    }
}
