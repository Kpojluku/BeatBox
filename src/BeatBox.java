import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class BeatBox {

    private JPanel mainPanel;
    private ArrayList<JCheckBox> checkBoxList;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private JFrame theFrame;
    private Path folder;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
        "Open Hi-Hat", "Acoustic Share", "Crash Cymbal", "Hand Clap",
        "High Tom", "Hi Bong", "Maracas", "Whistle", "Low Conga",
        "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args){
        new BeatBox().buildGUI();
    }

    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel backgroung = new JPanel(layout);
        backgroung.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkBoxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(e -> buildTrackAndStart());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> sequencer.stop());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(e -> {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor*1.03));
        });
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(e -> {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        });
        buttonBox.add(downTempo);

        JButton open = new JButton("Select a folder...");
        open.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(System.getProperty("user.home")+ "\\Desktop"));
            fc.setDialogTitle("Select a folder to save");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if(fc.showOpenDialog(open) == JFileChooser.APPROVE_OPTION){
                folder = Paths.get(fc.getSelectedFile().getAbsolutePath());
            }
        });
        buttonBox.add(open);

        JButton serialize = new JButton("Save");
        serialize.addActionListener(e -> {
            if(folder == null){
                JOptionPane.showMessageDialog(null, "Choose the directory to save");
                return;
            }
            boolean[] checkboxState = new boolean[256];

            for(int i = 0; i < 256; i++){
                JCheckBox check = checkBoxList.get(i);
                if(check.isSelected())
                    checkboxState[i]=true;
            }
            DateFormat dateFormat = new SimpleDateFormat("mm_ss");
            Calendar cal = Calendar.getInstance();
            try {
                FileOutputStream fileStream = new FileOutputStream(folder.toString() +
                        "\\BeatBox_" + dateFormat.format(cal.getTime()) +".txt");
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState);
                os.close();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
        buttonBox.add(serialize);

        JButton restore = new JButton("Download");
        restore.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(System.getProperty("user.home") + "\\Desktop"));
            fc.setDialogTitle("Select a folder to save");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if(fc.showOpenDialog(restore) == JFileChooser.APPROVE_OPTION){
                boolean[] checkboxState;
                try{
                    FileInputStream fileIn = new FileInputStream(fc.getSelectedFile().getAbsoluteFile());
                    ObjectInputStream is = new ObjectInputStream(fileIn);
                    checkboxState = (boolean[]) is.readObject();
                    is.close();

                    for(int i = 0;i<256;i++){
                        JCheckBox check = checkBoxList.get(i);
                        check.setSelected(checkboxState[i]);
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }
                sequencer.stop();
                buildTrackAndStart();
            }
        });
        buttonBox.add(restore);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i = 0; i<16; i ++){
            nameBox.add(new Label(instrumentNames[i]));
        }

        backgroung.add(BorderLayout.EAST, buttonBox);
        backgroung.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(backgroung);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        backgroung.add(BorderLayout.CENTER, mainPanel);

        for(int i = 0; i < 256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi(){
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart(){
        int[] trackList;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for( int i = 0; i < 16; i++){
            trackList = new int[16];

            int key = instruments[i];

            for(int j = 0; j<16; j++){

                JCheckBox jc = checkBoxList.get(j+(16*i));
                if(jc.isSelected()){
                    trackList[j]= key;
                }else {
                    trackList[j] = 0;
                }
            }

            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        }

        track.add(makeEvent(192, 9, 1, 0, 15));
        try{
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void makeTracks(int [] list){

        for(int i = 0; i<16; i++){
            int key = list[i];

            if(key !=0){
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        }catch (Exception e){
            e.printStackTrace();
        }
        return event;
    }
}
