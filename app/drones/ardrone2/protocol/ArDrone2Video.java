package drones.ardrone2.protocol;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import drones.ardrone2.util.DefaultPorts;
import drones.ardrone2.util.XugglerException;
import messages.ImageMessage;
import drones.shared.models.DroneConnectionDetails;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by brecht on 4/17/15.
 */
public class ArDrone2Video extends UntypedActor {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef listener;
    private final ActorRef parent;

    // Xuggler variables
    private IContainer container;
    private IConverter converter;
    private IStreamCoder videoCoder;
    private IVideoPicture toConvert;
    private IVideoResampler resampler;
    private IPacket packet;
    private int videoStreamId;
    private int numStreams;

    public ArDrone2Video(DroneConnectionDetails details, final ActorRef listener, final ActorRef parent) {
        // ArDrone 2 Model
        this.listener = listener;
        this.parent = parent;

        /***************************************************************************************************************
         * Following code is not the best use case for Akka. At the moment we haven't found a workaround to remove the *
         * used inputstream (Xuggler seems only to work well this way). Other h264 decoders aren't able to do what we  *
         * want.                                                                                                       *
         *                                                                                                             *
         * See: https://github.com/MahatmaX/YADrone/issues/15 for a discussion on different decoders                   *
         *  (The videostream from the ardrone2 contains a  proprietary header (PaVE). This header cannot be handled by *
         *   every decoder.)                                                                                           *
         **************************************************************************************************************/
        try (Socket skt = new Socket(details.getIp(), DefaultPorts.VIDEO_DATA.getPort())) {
            log.info("[ARDRONE2VIDEO] Starting ARDrone 2.0 Video");

            initDecoder(skt.getInputStream());
            decode();
            closeDecoder();
        } catch (UnknownHostException e) {
            log.error(e, "Could not find ArDrone2 host");
        } catch (IOException e) {
            log.error(e, "IOException in processing ArDrone2 video");
        } catch (XugglerException e) {
            log.error(e, "Exception in processing ArDrone2 video");
        }
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        log.error("[ARDRONE2VIDEO] Error");
    }

    /**
     * See: https://github.com/MahatmaX/YADrone/blob/master/YADrone/src/de/yadrone/base/video/xuggler/XugglerDecoder.java
     */
    private void decode() {
        while (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == videoStreamId) {
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());
                try {
                    int offset = 0;
                    while (offset < packet.getSize()) {
                        int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                        if (bytesDecoded < 0) {
                            throw new XugglerException("Got an error decoding single video frame");
                        }
                        offset += bytesDecoded;

                        if (picture.isComplete()) {
                            IVideoPicture newPic = picture;
                            toConvert = picture;

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
                            ImageIO.write(converter.toImage(picture), "JPEG", bos);

                            log.debug("[ARDONE2VIDEO] Video image decoded");

                            Object imageMessage = new ImageMessage(bos.toByteArray());
                            listener.tell(imageMessage, getSelf());
                        }
                    }
                } catch (XugglerException e) {
                    log.error(e, "Exception in processing ArDrone2 video");
                } catch (IOException e) {
                    log.error(e, "Exception in processing ArDrone2 video");
                }

            }

            // If parent (ArDrone2Protocol) is terminated, terminate this actor.
            if(parent.isTerminated()) {
                log.info("[ARDRONE2VIDEO] stopped (parent terminated)");
                getContext().stop(self());
                break;
            }
        }
    }

    /**
     * See: https://github.com/MahatmaX/YADrone/blob/master/YADrone/src/de/yadrone/base/video/xuggler/XugglerDecoder.java
     */
    private void initDecoder(InputStream is) throws XugglerException {
        if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new XugglerException("You must install the GPL version of Xuggler (with IVideoResampler support) for this to work");
        }

        container = IContainer.make();

        if (container.open(is, null) < 0) {
            throw new XugglerException("could not open inputstream");
        }

        numStreams = container.getNumStreams();

        videoStreamId = -1;
        videoCoder = null;
        for (int i = 0; i < numStreams; i++)
        {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
            {
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

        converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, IPixelFormat.Type.YUV420P, WIDTH, HEIGHT);
    }

    private void closeDecoder() {
        if (videoCoder != null) {
            videoCoder.close();
        }
        if (container != null) {
            container.close();
        }

        log.info("[ARDRONE2VIDEO] stopped");
        // Stop the actor
        getContext().stop(self());
    }
}
