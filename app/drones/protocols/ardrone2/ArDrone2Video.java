package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.xuggle.xuggler.*;
import drones.messages.ImageChangedMessage;
import drones.models.DroneConnectionDetails;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by brecht on 4/17/15.
 */
public class ArDrone2Video extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef listener;
    private final ActorRef parent;

    public ArDrone2Video(DroneConnectionDetails details, final ActorRef listener, final ActorRef parent) {
        // ArDrone 2 Model
        this.listener = listener;
        this.parent = parent;

        log.info("[ARDRONE2VIDEO] Starting ARDrone 2.0 Video");

        // @TODO change
        try (Socket skt = new Socket(details.getIp(), DefaultPorts.VIDEO_DATA.getPort())) {
            InputStream is = skt.getInputStream();
            decode(is);
        } catch (UnknownHostException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void decode(InputStream is) {
        if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new RuntimeException("you must install the GPL version of Xuggler (with IVideoResampler support) for this to work");
        }

        IContainer container = IContainer.make();

        if (container.open(is, null) < 0) {
            throw new IllegalArgumentException("could not open inpustream");
        }

        int numStreams = container.getNumStreams();

        int videoStreamId = -1;
        IStreamCoder videoCoder = null;
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
            throw new RuntimeException("could not find video stream");
        }

        if (videoCoder.open() < 0) {
            throw new RuntimeException("could not open video decoder for container");
        }

        IVideoResampler resampler = null;
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (resampler == null) {
                throw new RuntimeException("could not create color space resampler.");
            }
        }

        IPacket packet = IPacket.make();

        while (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == videoStreamId) {
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                try {
                    int offset = 0;
                    while (offset < packet.getSize()) {
                        int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                        if (bytesDecoded < 0) {
                            throw new RuntimeException("got an error decoding single video frame");
                        }
                        offset += bytesDecoded;

                        if (picture.isComplete()) {
                            IVideoPicture newPic = picture;

                            if (resampler != null) {
                                newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
                                if (resampler.resample(newPic, picture) < 0) {
                                    throw new RuntimeException("could not resample video");
                                }
                            }
                            if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
                                throw new RuntimeException("could not decode video as BGR 24 bit data");
                            }

                            // http://stackoverflow.com/questions/7178937/java-bufferedimage-to-png-format-base64-string
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ImageIO.write(Utils.videoPictureToImage(newPic), "png", Base64.getEncoder().wrap(bos));
                            String ImageB64 = bos.toString(StandardCharsets.ISO_8859_1.name());

                            log.debug("[ARDONE2VIDEO] Video image decoded");

                            Object imageMessage = new ImageChangedMessage(ImageB64);
                            listener.tell(imageMessage, getSelf());
                        }
                    }
                }
                catch(Exception exc) {
                    log.error(exc.getMessage());
                }
            }

            // If parent (ArDrone2Protocol) is terminated, terminate this actor.
            if(parent.isTerminated()) {
                log.info("[ARDRONE2VIDEO] stopped (parent terminated)");
                getContext().stop(self());
                break;
            }
        }

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

    @Override
    public void onReceive(Object msg) {
        log.error("Unknown message received");
        unhandled(msg);
    }
}
