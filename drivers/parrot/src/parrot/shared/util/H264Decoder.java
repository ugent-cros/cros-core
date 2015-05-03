package parrot.shared.util;

import akka.actor.ActorRef;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import droneapi.messages.ImageMessage;
import parrot.shared.util.XugglerException;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brecht on 4/30/15.
 */
public class H264Decoder extends Thread {
    private IContainer container;
    private IConverter converter;
    private IStreamCoder videoCoder;
    private IVideoResampler resampler;
    private IPacket packet;
    private int videoStreamId;

    private InputStream is;
    private int numberOfDecodedImages = 1;
    private boolean stop;

    private final ActorRef listener;

    public H264Decoder(InputStream is, ActorRef listener) {
        this.is = is;
        this.stop = false;
        this.listener = listener;
    }

    public void setStop() {
        stop = true;
    }

    /**
     * See: https://github.com/MahatmaX/YADrone/blob/master/YADrone/src/de/yadrone/base/video/xuggler/XugglerDecoder.java
     */
    private void initDecoder() throws XugglerException {
        if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new XugglerException("You must install the GPL version of Xuggler (with IVideoResampler support) for this to work");
        }

        container = IContainer.make();
        if (container.open(is, null) < 0) {
            throw new XugglerException("could not open inputstream");
        }

        int numStreams = container.getNumStreams();

        videoStreamId = -1;
        videoCoder = null;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
                break;
            }
        }
        if (videoStreamId == -1) {
            throw new XugglerException("could not find video stream");
        }

        if (videoCoder.open(null, null) < 0) {
            throw new XugglerException("could not open video decoder for container");
        }

        resampler = null;
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (resampler == null) {
                throw new XugglerException("could not create color space resampler.");
            }
        }

        packet = IPacket.make();

        IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
        converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, picture);
    }

    /**
     * See: https://github.com/MahatmaX/YADrone/blob/master/YADrone/src/de/yadrone/base/video/xuggler/XugglerDecoder.java
     */
    private void decode() {
        try {
            while (container.readNextPacket(packet) >= 0 || stop) {
                if (packet.getStreamIndex() == videoStreamId) {
                    IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());

                    int offset = 0;
                    while (offset < packet.getSize()) {
                        int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                        if (bytesDecoded < 0) {
                            throw new XugglerException("Got an error decoding single video frame");
                        }
                        offset += bytesDecoded;
                        if (picture.isComplete()) {
                            IVideoPicture newPic = picture;

                            if (resampler != null) {
                                newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
                                if (resampler.resample(newPic, picture) < 0) {
                                    throw new XugglerException("could not resample video");
                                }
                            }
                            if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
                                throw new XugglerException("could not decode video as BGR 24 bit data");
                            }

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            converter.toImage(picture);
                            ImageIO.write(converter.toImage(picture), "JPEG", bos);

                            // @TODO remove
                            System.out.println("[H264DECODER] Video image decoded [" + numberOfDecodedImages++ + "]");

                            Object imageMessage = new ImageMessage(bos.toByteArray());
                            listener.tell(imageMessage, null);
                        }
                    }
                }
            }
        } catch (XugglerException ex) {
            System.out.println("[H264DECODER] Exception");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("[H264DECODER] Exception");
            ex.printStackTrace();
        } finally {
            if (videoCoder != null) {
                videoCoder.close();
            }
            if (container != null) {
                container.close();
            }
        }
    }

    @Override
    public void run() {
        try {
            initDecoder();
            decode();
        } catch (XugglerException ex) {
            System.out.println("[H264DECODER] Exception");
            ex.printStackTrace();
        }
    }
}

