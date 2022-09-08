package FrontEnd;

import Devices.Device;
import MainComComponents.Bus;
import MainComComponents.CPUFlags;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class FrontControl {

    // device buttons
    @FXML
    private Button ROMButton;
    @FXML
    private Button RAMButton;
    @FXML
    private Button InputButton;
    @FXML
    private Button DispButton;
    @FXML
    private Button SoundButton;
    @FXML
    private Button AllButton;

    //devicepane
    @FXML
    private Pane devicePane;

    //CPU area
    //choicebox for debugger
    @FXML
    private ChoiceBox<Device> debuggerDropdown;
    @FXML
    private Button debuggerShowAllButt;

    //debugger textarea
    @FXML
    private TextArea debuggerTA;
    private Device debuggerLookAt;

    //Register Viewer
    @FXML
    private Label AXYLabel;
    @FXML
    private Label PCLabel;
    @FXML
    private Label SPLabel;
    //statregs
    @FXML
    private Label SRCLabel;
    @FXML
    private Label SRZLabel;
    @FXML
    private Label SRILabel;
    @FXML
    private Label SRDLabel;
    @FXML
    private Label SRBLabel;
    @FXML
    private Label SRULabel;
    @FXML
    private Label SRVLabel;
    @FXML
    private Label SRNLabel;

    @FXML
    private Label clockCycleCount;



    @FXML
    //initialising method for JFX
    public void initialize() {

        //add tooltip to debuggerShowAllButt
        Tooltip DSABTooltip = new Tooltip("NOTE!\nTHIS IS VERY UNINTUITIVE AND LAGGY!");
        DSABTooltip.setStyle("-fx-background-color: red; -fx-text-alignment: center; -fx-font: bold 14 sans-serif");
        DSABTooltip.setShowDelay(Duration.millis(10));
        debuggerShowAllButt.setTooltip(DSABTooltip);

        //hook choicebox to devices array list;
        debuggerDropdown.setItems(Bus.devices);

        //add a listener to choicebox that updates debugger TA
        debuggerDropdown.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldVal, newVal) -> {
                    debuggerLookAt = newVal;
                    updateDebuggerTA();
                }
        );

        //initialise Registers panel
        updateRegistersPanel();

    }

    @FXML
    protected void onROMButtonClick(){
        DeviceController dc = () -> {
                VBox returnedPane = new VBox();
                returnedPane.getChildren().add(new Label("ROMButton"));
                return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onRAMButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();

            TextField addressTF = new TextField();
            addressTF.setPromptText("short address (hex value)");

            TextField valueTF = new TextField();
            valueTF.setPromptText("set byte value (hex value)");

            Button b = new Button("Set Value");

            b.setOnAction( eh -> {
                short editAddr;
                byte editVal;
                try{
                    editAddr = (short)Integer.parseInt(addressTF.getText(),16);
                    editVal = (byte) Integer.parseInt(valueTF.getText(),16);

                    Bus.serveDataToAdr(editAddr,editVal);
                } catch (NumberFormatException nfe) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Bad Value!");
                    a.setHeaderText("values for address and edit value are bad!");
                    a.showAndWait();
                }
                updateDebuggerTA();
            });

            returnedPane.getChildren().add(new Label("RAM Editor"));
            returnedPane.getChildren().add(addressTF);
            returnedPane.getChildren().add(valueTF);
            returnedPane.getChildren().add(b);

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onInputButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();
            returnedPane.getChildren().add(new Label("InputButton"));
            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onDispButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();
            returnedPane.getChildren().add(new Label("DispButton"));
            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onSoundButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();
            returnedPane.getChildren().add(new Label("SoundButton"));
            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onAllButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();
            returnedPane.getChildren().add(new Label("AllButton"));
            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    public void updateDebuggerTA(){
        //debuggerTA.clear();
        StringBuilder sb = new StringBuilder();
        if(debuggerLookAt != null) {
            short currAddr = debuggerLookAt.getStartAddress();
            currAddr--;
            do {
                currAddr++;
                //if(currAddr % 1000 == 0) System.out.println(currAddr);
                String addrHex = Integer.toHexString(Short.toUnsignedInt(currAddr));
                byte memValue = Bus.serveDataFromAdr(currAddr);
                String hexString = Integer.toHexString(Byte.toUnsignedInt(memValue));
                String binString = Integer.toBinaryString(Byte.toUnsignedInt(memValue));
                sb.append(String.format("0x%4s:_0x%2s_0b%8s%n", addrHex, hexString, binString));
            } while (currAddr != debuggerLookAt.getEndAddress());
        } else {
            short currAddr = -1;
            do {
                currAddr++;
                String addrHex = Integer.toHexString(Short.toUnsignedInt(currAddr));
                byte memValue = Bus.serveDataFromAdr(currAddr);
                String hexString = Integer.toHexString(Byte.toUnsignedInt(memValue));
                String binString = Integer.toBinaryString(Byte.toUnsignedInt(memValue));
                sb.append(String.format("0x%4s:_0x%2s_0b%8s%n", addrHex, hexString, binString));
            } while (currAddr != (short) 0xFFFF);
        }
        debuggerTA.setText(sb.toString().replace(' ','0').replace('_',' '));
    }

    protected void updateRegistersPanel(){
        byte a, x, y, sp, sr;
        short pc;

        a = Bus.processor.getAccumulator();
        x = Bus.processor.getXReg();
        y = Bus.processor.getYReg();
        sp = Bus.processor.getStackPointer();
        sr = Bus.processor.getStatRegs();
        pc = Bus.processor.getProgramCounter();

        //get hex and bin strings
        String hexA = Integer.toHexString(Byte.toUnsignedInt(a));
        String binA = Integer.toBinaryString(Byte.toUnsignedInt(a));

        String hexX = Integer.toHexString(Byte.toUnsignedInt(x));
        String binX = Integer.toBinaryString(Byte.toUnsignedInt(x));

        String hexY = Integer.toHexString(Byte.toUnsignedInt(y));
        String binY = Integer.toBinaryString(Byte.toUnsignedInt(y));

        String hexSP = Integer.toHexString(Byte.toUnsignedInt(sp));

        String hexPC = Integer.toHexString(Short.toUnsignedInt(pc));

        //construct the strings to be displayed
        String AXYString = String.format("0x%2s_0b%8s%n", hexA, binA) +
                            String.format("0x%2s_0b%8s%n", hexX, binX) +
                            String.format("0x%2s_0b%8s", hexY, binY);

        String PCString = String.format("0x%4s", hexPC);

        String SPString = String.format("0x01%2s", hexSP);


        //display the strings
        AXYLabel.setText(AXYString.replace(' ','0').replace('_',' '));
        PCLabel.setText(PCString.replace(' ','0'));
        SPLabel.setText(SPString.replace(' ','0'));

        //the stat reg part
        //red for off, green for on
        //Carry
        SRCLabel.setTextFill((Bus.processor.getFlag(CPUFlags.CARRY) == 1) ? Color.LIME : Color.RED);
        //Zero
        SRZLabel.setTextFill((Bus.processor.getFlag(CPUFlags.ZERO) == 1) ? Color.LIME : Color.RED);
        //(disable) Interupt
        SRILabel.setTextFill((Bus.processor.getFlag(CPUFlags.D_INTERRUPT) == 1) ? Color.LIME : Color.RED);
        //Decimal
        SRDLabel.setTextFill((Bus.processor.getFlag(CPUFlags.DECIMAL) == 1) ? Color.LIME : Color.RED);
        //Break
        SRBLabel.setTextFill((Bus.processor.getFlag(CPUFlags.BREAK) == 1) ? Color.LIME : Color.RED);
        //Unused
        SRULabel.setTextFill((Bus.processor.getFlag(CPUFlags.UNUSED) == 1) ? Color.LIME : Color.RED);
        //oVerflow
        SRVLabel.setTextFill((Bus.processor.getFlag(CPUFlags.OVERFLOW) == 1) ? Color.LIME : Color.RED);
        //Negative
        SRNLabel.setTextFill((Bus.processor.getFlag(CPUFlags.NEGATIVE) == 1) ? Color.LIME : Color.RED);

    }

    @FXML
    protected void stepClockOnAction(){
        Bus.processor.clock();
        updateDebuggerTA();
        updateRegistersPanel();
        clockCycleCount.setText(String.valueOf(Bus.processor.clock_count));
    }

    @FXML
    protected void resetCPUButtonAction(){
        Bus.processor.reset();
        updateDebuggerTA();
        updateRegistersPanel();
        clockCycleCount.setText(String.valueOf(Bus.processor.clock_count));
    }

    @FXML
    protected void debuggerShowAllButtAction(){
        this.debuggerLookAt = null;
        updateDebuggerTA();
    }

}