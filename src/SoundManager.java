import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {

    @SuppressWarnings("restriction")
    public SoundManager(String path) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(path).getAbsoluteFile());
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);
        clip.start();
    }
}