package FrontEnd;

import Devices.*;
import Extras.DeviceComparator;
import MainComComponents.Bus;
import MainComComponents.CPUFlags;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class FrontControl {

    // the pane that encompasses all
    @FXML
    private AnchorPane allPane;

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
    private VBox devicePane;

    //CPU area
    //choicebox for debugger
    @FXML
    private ComboBox<Device> debuggerDropdown;
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
    private TextField clockCycleCount;

    //autoclocker
    @FXML
    private TextField autoClockTF;
    @FXML
    private Button autoClockButt;
    private Timeline autoClockTimeline;
    private boolean autoClockActive;

    // stuff for input
    private Input inputObject;

    private ResourceBundle currentTextRB = ResourceBundle.getBundle("words");


    // TODO: 9/30/2022 add internationalisation 

    
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

        //try to default debugger to 0th device
        try {
            debuggerDropdown.getSelectionModel().select(0);
        } catch (Exception ex) {
            //if there is no devices in Bus.Devices to begin with
        }

        //initialise Registers panel
        updateRegistersPanel();


        //set up tooltip for autoClockTF
        //Tooltip ACTFTooltip = new Tooltip("The automatic clock is only accurate to 1KHz"); //up for testing
        Tooltip ACTFTooltip = new Tooltip("Input a Double value");
        ACTFTooltip.setStyle("-fx-font: 12 sans-serif");
        ACTFTooltip.setShowDelay(Duration.millis(10));
        autoClockTF.setTooltip(ACTFTooltip);


        //setup empty autoclock
        autoClockTimeline = new Timeline();


        // create a new Input Object (there will only be one per computer)
        // default start address will be 0x00_00

        inputObject = new Input("Input", (short) 0x0000);

        //set key handler for key press
        allPane.setOnKeyPressed(eh -> {
            //if the input object is on the bus
            if (Bus.devices.contains(inputObject)) {
                //if keypress has not yet been registered
                if (!inputObject.isKeyPressRegistered()) {
                    //if all characters are to be detected OR
                    //if the keypress is in the allowed characters list
                    if (inputObject.getAllowedCharacters() == null
                            || inputObject.getAllowedCharacters().contains(eh.getCode().getChar().charAt(0))) {
                        inputObject.registerKeyPress(eh.getCode());

                        //if we should send a IRQ for this keypress,
                        if (inputObject.isSendKeyPressInterrupts()) {
                            Bus.processor.IRQ();
                        }
                    }
                    inputObject.setKeyPressRegistered(true);
                    //update the debug pane
                    updateDebuggerTA();
                }
            }
        });
        allPane.setOnKeyReleased( eh -> {
            //if the input object is on the bus
            if(Bus.devices.contains(inputObject)) {
                if (!inputObject.isStickyKeys()) {
                    //reset to 0x0000 using key 0x0000
                    inputObject.clearKey();
                }
                //open for new keypress
                inputObject.setKeyPressRegistered(false);
                //update the debug pane if Input item is on the bus
                if (Bus.devices.contains(inputObject)) updateDebuggerTA();
            }
        });


    }

    @FXML
    protected void onROMButtonClick(){
        DeviceController dc = () -> {
            AtomicReference<ROM> selectedROM = new AtomicReference<>();

            ChoiceBox<ROM> ROMCB = new ChoiceBox<>();
            Tooltip ROMCBTooltip = new Tooltip(currentTextRB.getString("ROMCBTooltip"));
            ROMCBTooltip.setStyle("-fx-text-alignment: center; -fx-font: 13 sans-serif");
            ROMCBTooltip.setShowDelay(Duration.millis(10));
            ROMCB.setTooltip(ROMCBTooltip);
            ROMCB.setPrefHeight(new Text("ROM\nROM").getLayoutBounds().getHeight() + 10);
            //initialise contents of ROMCB when ROM menu is entered
            ROMCB.getItems().clear();
            for (Device d: Bus.devices) {
                if(d.getClass().getSimpleName().equals("ROM")){
                    ROMCB.getItems().add((ROM) d);
                }
            }

            //check for and add new ROM objects if Bus.devices changes
            Bus.devices.addListener((ListChangeListener<Device>) change -> {
                ROMCB.getItems().clear();
                for (Device d: Bus.devices) {
                    if(d.getClass().getSimpleName().equals("ROM")){
                        ROMCB.getItems().add((ROM) d);
                    }
                }
            });
            //set the selectedROM on selection
            ROMCB.getSelectionModel().selectedItemProperty().addListener(
                    (observableValue, oldROM, newROM) -> {
                        selectedROM.set(newROM);
                        debuggerDropdown.getSelectionModel().select(newROM);
                    }
            );


            TextArea BINfilepathTF = new TextArea();
            BINfilepathTF.setPromptText("Please Enter FULL path to BIN file");
            BINfilepathTF.setWrapText(true);
            BINfilepathTF.setPrefRowCount(3);

            Button writeROMButt = new Button("Flash ROM with BIN file");

            writeROMButt.setOnAction( eh -> {
                //get filepath text

                String BINfilepath = BINfilepathTF.getText().toUpperCase();

                //check if it is BIN file
                if(!Pattern.compile("^(.*)\\b.BIN\\b$").matcher(BINfilepath).matches()){
                    //if the file is not bin file
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Bad File!");
                    a.setHeaderText("filepath provided does not link to a BIN file!");
                    a.showAndWait();
                }
                //if the file is a BIN file, check if it exists
                else if(!(new File(BINfilepath).exists())){
                    //if the file does not exist
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Bad File!");
                    a.setHeaderText("a BIN file does not exist at given filepath!");
                    a.showAndWait();
                }
                //check if a ROM object is selected
                else if(selectedROM.get() == null){
                    //if no ROM chosen
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("no ROM chosen!");
                    a.setHeaderText("choose a ROM device to edit from the dropdown box");
                    a.showAndWait();
                }
                //if the file is BIN and it exists and a ROM object to edit is chosen
                else {
                    try (
                            InputStream inputStream = new FileInputStream(BINfilepath)
                    ) {
                        byte[] inBytes = inputStream.readAllBytes();

                        //push a warning if ROM has less space than BIN file
                        if(selectedROM.get().getROMSize() < inBytes.length){
                            Alert a = new Alert(Alert.AlertType.WARNING);
                            a.setTitle("Too Much Data!");
                            a.setHeaderText("You are writing more Bytes than the selected ROM can store!\n"
                                            + "We will only write the first " + selectedROM.get().getROMSize() + "Bytes of your file.");
                            a.showAndWait();
                        }

                        //flash the ROM
                        selectedROM.get().flashROM(inBytes);

                        //update debugger to view the edited ROM
                        debuggerDropdown.getSelectionModel().select(selectedROM.get());
                        updateDebuggerTA();

                    } catch (ArithmeticException ArE){
                        //if the file does not exist
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bad File!");
                        a.setHeaderText("Your file is too big! \nWe accept files up to 2147483647 Bytes in size only!");
                        a.showAndWait();
                    } catch (IOException ex) {
                        //if there is an IOException
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("IOException!");
                        a.setHeaderText("Somehow an IOException Occurred???");
                        a.setContentText(ex.getMessage());
                        a.showAndWait();
                    }
                }
            });

            Button wipeROMButt = new Button("Wipe ROM");

            wipeROMButt.setOnAction(eh -> {
                //check if a ROM object is selected
                if(selectedROM.get() == null){
                    //if no ROM chosen
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("no ROM chosen!");
                    a.setHeaderText("choose a ROM device to edit from the dropdown box");
                    a.showAndWait();
                } else {
                    byte[] emptyROM = new byte[selectedROM.get().getROMSize()];
                    selectedROM.get().flashROM(emptyROM);
                    //update debugger to view the wiped ROM
                    debuggerDropdown.getSelectionModel().select(selectedROM.get());
                    updateDebuggerTA();
                }
            });



            //create the pane and add children
            VBox returnedPane = new VBox();

            Label menuLabel = new Label("ROM Flasher");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);
            returnedPane.getChildren().add(ROMCB);
            returnedPane.getChildren().add(BINfilepathTF);
            returnedPane.getChildren().add(writeROMButt);
            returnedPane.getChildren().add(wipeROMButt);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color:  #ff860d; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onRAMButtonClick(){
        DeviceController dc = () -> {
            AtomicReference<RAM> selectedRAM = new AtomicReference<>();

            TextArea RAMDisp = new TextArea("Select A RAM Device to view");
            RAMDisp.setEditable(false);
            RAMDisp.setPrefHeight((new Text("R\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\nR\n")
                    .getLayoutBounds().getHeight() + 10)*(18.0/12.0));
            RAMDisp.setStyle("-fx-control-inner-background: black; -fx-font-size: 18; -fx-font-family: consolas;");
            //refresh RAMDisp when clocked
            //we can detect if a clock has happened by detecting a change in clock count number
            this.clockCycleCount.textProperty().addListener((obs,oldV,newV) -> {
                //remember ramdisp scroll position
                double scrollPosition = RAMDisp.scrollTopProperty().doubleValue();
                StringBuilder sb = new StringBuilder();
                if(selectedRAM.get() != null) {
                    short currAddr = selectedRAM.get().getStartAddress();
                    currAddr--;
                    do {
                        currAddr++;
                        //if(currAddr % 1000 == 0) System.out.println(currAddr);
                        String addrHex = Integer.toHexString(Short.toUnsignedInt(currAddr));
                        byte memValue = Bus.serveDataFromAdr(currAddr);
                        String hexString = Integer.toHexString(Byte.toUnsignedInt(memValue));
                        String binString = Integer.toBinaryString(Byte.toUnsignedInt(memValue));
                        sb.append(String.format("0x%4s:_0x%2s_0b%8s%n", addrHex, hexString, binString));
                    } while (currAddr !=  selectedRAM.get().getEndAddress());
                } else {
                    sb.append("Select_A_RAM_Device_to_view.");
                }
                RAMDisp.setText(sb.toString().replace(' ','0').replace('_',' '));
                //restore ram disp scroll position
                RAMDisp.setScrollTop(scrollPosition);
            });

            ChoiceBox<RAM> RAMCB = new ChoiceBox<>();
            RAMCB.setPrefHeight(new Text("RAM\nRAM").getLayoutBounds().getHeight() + 10);
            Tooltip RAMCBTooltip = new Tooltip(currentTextRB.getString("RAMCBTooltip"));
            RAMCBTooltip.setStyle("-fx-text-alignment: center; -fx-font: 13 sans-serif");
            RAMCBTooltip.setShowDelay(Duration.millis(10));
            RAMCB.setTooltip(RAMCBTooltip);
            //initialise contents of RAMCB when RAM menu is entered
            RAMCB.getItems().clear();
            for (Device d: Bus.devices) {
                if(d.getClass().getSimpleName().equals("RAM")){
                    RAMCB.getItems().add((RAM) d);
                }
            }

            //check for and add new RAM objects if Bus.devices changes
            Bus.devices.addListener((ListChangeListener<Device>) change -> {
                RAMCB.getItems().clear();
                for (Device d: Bus.devices) {
                    if(d.getClass().getSimpleName().equals("RAM")){
                        RAMCB.getItems().add((RAM) d);
                    }
                }
            });
            //set the selectedROM on selection
            RAMCB.getSelectionModel().selectedItemProperty().addListener(
                    (observableValue, oldROM, newROM) -> {
                        selectedRAM.set(newROM);

                        //update RAMDisp
                        //remember ramdisp scroll position
                        double scrollPosition = RAMDisp.scrollTopProperty().doubleValue();
                        StringBuilder sb = new StringBuilder();
                        if(selectedRAM.get() != null) {
                            short currAddr = selectedRAM.get().getStartAddress();
                            currAddr--;
                            do {
                                currAddr++;
                                //if(currAddr % 1000 == 0) System.out.println(currAddr);
                                String addrHex = Integer.toHexString(Short.toUnsignedInt(currAddr));
                                byte memValue = Bus.serveDataFromAdr(currAddr);
                                String hexString = Integer.toHexString(Byte.toUnsignedInt(memValue));
                                String binString = Integer.toBinaryString(Byte.toUnsignedInt(memValue));
                                sb.append(String.format("0x%4s:_0x%2s_0b%8s%n", addrHex, hexString, binString));
                            } while (currAddr != selectedRAM.get().getEndAddress());
                        } else {
                            sb.append("Select_A_RAM_Device_to_view.");
                        }
                        RAMDisp.setText(sb.toString().replace(' ','0').replace('_',' '));
                        //restore ram disp scroll position
                        RAMDisp.setScrollTop(scrollPosition);
                    }
            );

            Button resetButt = new Button("Reset RAM");

            resetButt.setOnAction( eh -> {
                if(selectedRAM.get() != null) {
                    selectedRAM.get().resetRAM();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("NO RAM DEVICE SELECTED!");
                    a.setHeaderText("Select a RAM device to wipe.");
                    a.showAndWait();
                }

                //update RAMDisp & debugger after this
                //remember ramdisp scroll position
                double scrollPosition = RAMDisp.scrollTopProperty().doubleValue();
                StringBuilder sb = new StringBuilder();
                if(selectedRAM.get() != null) {
                    short currAddr = selectedRAM.get().getStartAddress();
                    currAddr--;
                    do {
                        currAddr++;
                        //if(currAddr % 1000 == 0) System.out.println(currAddr);
                        String addrHex = Integer.toHexString(Short.toUnsignedInt(currAddr));
                        byte memValue = Bus.serveDataFromAdr(currAddr);
                        String hexString = Integer.toHexString(Byte.toUnsignedInt(memValue));
                        String binString = Integer.toBinaryString(Byte.toUnsignedInt(memValue));
                        sb.append(String.format("0x%4s:_0x%2s_0b%8s%n", addrHex, hexString, binString));
                    } while (currAddr != selectedRAM.get().getEndAddress());
                } else {
                    sb.append("Select A RAM Device to view.");
                }
                RAMDisp.setText(sb.toString().replace(' ','0').replace('_',' '));
                //restore ram disp scroll position
                RAMDisp.setScrollTop(scrollPosition);

                //update debugger TA
                updateDebuggerTA();
            });

            VBox returnedPane = new VBox();

            Label menuLabel = new Label("RAM Viewer");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);
            returnedPane.getChildren().add(RAMCB);
            returnedPane.getChildren().add(RAMDisp);
            returnedPane.getChildren().add(resetButt);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #ff5429; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    @FXML
    protected void onInputButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();

            Label currentAddrL = new Label();
            currentAddrL.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 16; -fx-text-background-color: white");
            {
                String startAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress()));
                String endAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getEndAddress()));

                currentAddrL.setText(String.format("Current_Address_:_0x%4s_0x%4s",startAddrString,endAddrString)
                        .replace(' ','0').replace('_',' '));
            }


            TextField addressTF = new TextField();
            addressTF.setPromptText(String.format("Current_Start_Address_:_0x%4s._",
                            Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress())))
                    .replace(' ','0').replace('_',' ') + "Type in new start address here.");

            ToolBar addrEditorButts = new ToolBar();

            Button addressButt = new Button("Set New Address");

            addressButt.setOnAction( eh -> {
                short editAddr;

                String newStartAddrString = addressTF.getText().replaceFirst("0x", "");

                try{
                    editAddr = (short)Integer.parseInt(newStartAddrString,16);

                    boolean isAddrTaken = false;

                    for(Device d : Bus.devices){
                        // check if proposed address is taken
                        if(d != inputObject
                                && ((Short.toUnsignedInt(editAddr) >= Short.toUnsignedInt(d.getStartAddress())
                                && Short.toUnsignedInt(editAddr) <= Short.toUnsignedInt(d.getEndAddress()))
                                || (Short.toUnsignedInt((short)(editAddr+1)) >= Short.toUnsignedInt(d.getStartAddress())
                                && Short.toUnsignedInt((short)(editAddr+1)) <= Short.toUnsignedInt(d.getEndAddress())))) {
                            isAddrTaken = true; break;
                        }
                    }

                    if(!(Pattern.compile("[A-Fa-f0-9]{1,4}$").matcher(newStartAddrString).matches())) {
                        //check input length
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bad Value!");
                        a.setHeaderText(currentTextRB.getString("badShortInputLengthErrorMsg"));
                        a.showAndWait();
                    } else if (editAddr == (short)0xffff) {
                        // firstly, are you trying to 0xffff to 0x0000 me
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bad Value!");
                        a.setHeaderText("Input Device needs at least 2 free address spaces!");
                        a.showAndWait();
                    } else if(isAddrTaken){
                        // is new address space is already taken by other devices
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bad Value!");
                        a.setHeaderText("Proposed Address space is already taken!");
                        a.showAndWait();
                    } else {
                        // else set new address & update the Label
                        inputObject.setStartAddress(editAddr);

                        String startAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress()));
                        String endAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getEndAddress()));

                        //update label and addressTF
                        currentAddrL.setText(String.format("Current_Address_:_0x%4s_0x%4s",startAddrString,endAddrString)
                                .replace(' ','0').replace('_',' '));
                        addressTF.setPromptText(String.format("Current_Start_Address_:_0x%4s._",
                                        Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress())))
                                .replace(' ','0').replace('_',' ') + "Type in new start address here.");

                        //clear addressTF
                        addressTF.clear();
                    }

                } catch (NumberFormatException nfe) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Bad Value!");
                    a.setHeaderText("values given for new address is bad!");
                    a.showAndWait();
                }
                //refresh the choice box menu if inputObject on the Bus
                if(Bus.devices.contains(inputObject)) {
                    Device currSel = debuggerLookAt;
                    Bus.devices.remove(inputObject);
                    Bus.devices.add(inputObject);
                    debuggerDropdown.getSelectionModel().select(currSel);
                }
                updateDebuggerTA();
            });

            ToggleButton busConnectTB = new ToggleButton("Connect Input Device To Bus");

            if(Bus.devices.contains(inputObject)) busConnectTB.setSelected(true);

            busConnectTB.setOnAction(eh -> {
                if(busConnectTB.isSelected()){
                    // check if current address space is taken before connecting

                    boolean isAddrTaken = false;

                    for(Device d : Bus.devices){
                        // check if proposed address is taken
                        if(d != inputObject
                                && (Short.toUnsignedInt(inputObject.getStartAddress()) >= Short.toUnsignedInt(d.getStartAddress())
                                && Short.toUnsignedInt(inputObject.getStartAddress()) <= Short.toUnsignedInt(d.getEndAddress()))
                                || Short.toUnsignedInt(inputObject.getEndAddress()) >= Short.toUnsignedInt(d.getStartAddress())
                                && Short.toUnsignedInt(inputObject.getEndAddress()) <= Short.toUnsignedInt(d.getEndAddress())) {
                            isAddrTaken = true; break;
                        }
                    }

                    if(isAddrTaken){
                        // is address space is already taken by other devices
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Wait A Minute!");
                        a.setHeaderText("The Address Space you allocated to this Input Object is unavailable!");
                        a.showAndWait();
                        busConnectTB.setSelected(false);
                    } else {
                        // else conncect to the bus and update the label
                        Bus.devices.add(inputObject);

                        String startAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress()));
                        String endAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getEndAddress()));

                        currentAddrL.setText(String.format("Current_Address_:_0x%4s_0x%4s",startAddrString,endAddrString)
                                .replace(' ','0').replace('_',' '));
                    }
                } else {
                    //disconnect if already connected
                    Bus.devices.remove(inputObject);
                    //redraw the debug screen after defaulting to first object if the current object is inputObject
                    if(debuggerLookAt == inputObject || debuggerLookAt == null)
                        debuggerDropdown.getSelectionModel().select(0);
                    updateDebuggerTA();
                }
            });

            addrEditorButts.getItems().add(addressButt);
            addrEditorButts.getItems().add(busConnectTB);
            addrEditorButts.setStyle("-fx-alignment: center; -fx-background-color: none;");


            TextField detectCharsTF = new TextField();
            detectCharsTF.setPromptText("Type in characters to detect");
            if(inputObject.getAllowedCharacters() != null){
                char[] allowedChars = new char[inputObject.getAllowedCharacters().size()];
                for (int i = 0; i < allowedChars.length; i++) {
                    allowedChars[i] = inputObject.getAllowedCharacters().get(i);
                }
                detectCharsTF.setText(new String(allowedChars));
            }

            ToolBar detectCharsButts= new ToolBar();

            Button setCharsToDetectButt = new Button("Set Characters");
            setCharsToDetectButt.setStyle("-fx-border-width: 2; -fx-border-color: DeepSkyBlue; -fx-background-insets: 1");

            Button detectAllButt = new Button("Detect All Characters");
            detectAllButt.setStyle("-fx-border-width: 2; -fx-border-color: none; -fx-background-insets: 1");

            setCharsToDetectButt.setOnAction( eh -> {
                inputObject.setAllowedCharacters(
                        (detectCharsTF.getText().toLowerCase() + detectCharsTF.getText().toUpperCase()).toCharArray());
                setCharsToDetectButt.setStyle("-fx-border-color: DeepSkyBlue; -fx-border-width: 2");
                detectAllButt.setStyle("-fx-border-color:  none;");
            });
            detectAllButt.setOnAction( eh -> {
                inputObject.allAllowedCharacters();
                detectAllButt.setStyle("-fx-border-color: DeepSkyBlue; -fx-border-width: 2");
                setCharsToDetectButt.setStyle("-fx-border-color:  none;");
            });

            if(inputObject.getAllowedCharacters() == null) {
                detectAllButt.setStyle("-fx-border-color: DeepSkyBlue; -fx-border-width: 2");
                setCharsToDetectButt.setStyle("-fx-border-color:  none;");
            }

            detectCharsButts.getItems().add(setCharsToDetectButt);
            detectCharsButts.getItems().add(detectAllButt);
            detectCharsButts.setStyle("-fx-alignment: center; -fx-background-color: none;");


            ToolBar otherInputSettingsButts= new ToolBar();

            ToggleButton toSendIRQTB = new ToggleButton("send IRQ on keypress?");
            if(inputObject.isSendKeyPressInterrupts()) toSendIRQTB.setSelected(true);
            toSendIRQTB.setOnAction(eh -> inputObject.setSendKeyPressInterrupts(toSendIRQTB.isSelected()));

            ToggleButton stickyKeysTB = new ToggleButton("StickyKeys");
            if(inputObject.isStickyKeys()) stickyKeysTB.setSelected(true);
            Tooltip stickyKeysTT = new Tooltip("if selected, keycode will not get cleared when key is released.\n" +
                    "if not selected, keycode will reset to 0x0000 on release");
            stickyKeysTT.setStyle("-fx-font: 12 sans-serif");
            stickyKeysTB.setTooltip(stickyKeysTT);
            stickyKeysTB.setOnAction(eh -> inputObject.setStickyKeys(stickyKeysTB.isSelected()));

            otherInputSettingsButts.getItems().add(toSendIRQTB);
            otherInputSettingsButts.getItems().add(stickyKeysTB);
            otherInputSettingsButts.setStyle("-fx-alignment: center; -fx-background-color: none;");



            Label menuLabel = new Label("Input Device (Only One!)");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);
            returnedPane.getChildren().add(currentAddrL);
            returnedPane.getChildren().add(addressTF);
            returnedPane.getChildren().add(addrEditorButts);
            returnedPane.getChildren().add(detectCharsTF);
            returnedPane.getChildren().add(detectCharsButts);
            returnedPane.getChildren().add(otherInputSettingsButts);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #780373; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };


        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());

    }

    @FXML
    protected void onDispButtonClick(){
        DeviceController dc = () -> {
            AtomicReference<Display> selectedDisplay = new AtomicReference<>();
            //boolean variable to let modeCB know whether to change mode
            AtomicBoolean justSwitchedDisplay = new AtomicBoolean(false);

            ImageView iv = new ImageView();
            iv.setFitHeight(3*128); iv.setFitWidth(3*128);
            //select image
            iv.setImage(
                    new Image(Objects.requireNonNull(FrontControl.class
                            .getResourceAsStream("/FrontEnd/select display to view.png"))));

            ChoiceBox<Display> DisplayCB = new ChoiceBox<>();
            Tooltip DisplayCBTooltip = new Tooltip(currentTextRB.getString("DisplayCBTooltip"));
            DisplayCBTooltip.setStyle("-fx-text-alignment: center; -fx-font: 13 sans-serif");
            DisplayCBTooltip.setShowDelay(Duration.millis(10));
            DisplayCB.setTooltip(DisplayCBTooltip);
            DisplayCB.setPrefHeight(new Text("DIS\nDIS").getLayoutBounds().getHeight() + 10);

            //define the mode choicebox now to make things easy
            ChoiceBox<String> modeCB = new ChoiceBox<>();
            Tooltip displayModeCBTooltip = new Tooltip(currentTextRB.getString("displayModeCBTooltip"));
            displayModeCBTooltip.setStyle("-fx-text-alignment: center; -fx-font: 13 sans-serif");
            displayModeCBTooltip.setShowDelay(Duration.millis(10));
            modeCB.setTooltip(displayModeCBTooltip);
            modeCB.setDisable(true);
            modeCB.getItems().addAll(
                    "64x64 1 bit BW",
                    "64x64 6 bit RGB",
                    "128x128 1 bit BW",
                    "128x128 6 bit RGB");

            //initialise contents of DisplayCB when Display menu is entered
            DisplayCB.getItems().clear();
            for (Device d: Bus.devices) {
                if(d.getClass().getSimpleName().equals("Display")){
                    DisplayCB.getItems().add((Display) d);
                }
            }

            //check for and add new Display objects if Bus.devices changes
            Bus.devices.addListener((ListChangeListener<Device>) change -> {
                DisplayCB.getItems().clear();
                for (Device d: Bus.devices) {
                    if(d.getClass().getSimpleName().equals("Display")){
                        DisplayCB.getItems().add((Display) d);
                    }
                }
            });
            //set the selectedDisplay on selection
            //also enable mode selector
            DisplayCB.getSelectionModel().selectedItemProperty().addListener(
                    (observableValue, oldDisp, newDisp) -> {
                        selectedDisplay.set(newDisp);
                        if (selectedDisplay.get() != null) {
                            iv.setImage(selectedDisplay.get().getFrame());
                            modeCB.setDisable(false);
                            justSwitchedDisplay.set(true);
                            if (selectedDisplay.get().getVRAMSize() == 512) modeCB.getSelectionModel().select(0);
                            if (selectedDisplay.get().getVRAMSize() == 4096) modeCB.getSelectionModel().select(1);
                            if (selectedDisplay.get().getVRAMSize() == 2048) modeCB.getSelectionModel().select(2);
                            if (selectedDisplay.get().getVRAMSize() == 16348) modeCB.getSelectionModel().select(3);
                            justSwitchedDisplay.set(false);
                        }
                    }
            );


            //0 "64x64 BW",         [512 bytes]
            //1 "64x64 6 bit RGB",  [4096  bytes]
            //2 "128x128 6 bit BW", [2048  bytes]
            //3 "128x128 6 bit RGB" [16384 bytes]
            //set display mode & refresh display
            modeCB.getSelectionModel().selectedItemProperty().addListener(
                    (observableValue, oldMode, newMode) -> {
                        if(!justSwitchedDisplay.get()) {
                            //only set mode if change is NOT due to initialisation
                            if (modeCB.getSelectionModel().getSelectedIndex() == 0) {
                                if (Short.toUnsignedInt(selectedDisplay.get().getStartAddress()) > (65535 - 511)) {
                                    //check if changing mode will go out of bounds
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Wait A Minute!");
                                    a.setHeaderText("Not sufficient Address Spaces left to change to this mode!");
                                    a.showAndWait();
                                    justSwitchedDisplay.set(true);
                                    modeCB.getSelectionModel().select(oldMode);
                                    justSwitchedDisplay.set(false);
                                } else {
                                    //check if changing mode intrudes into other's space
                                    boolean isAddrTaken = false;

                                    for (Device d : Bus.devices) {
                                        // check if proposed address is taken
                                        if (d != selectedDisplay.get()
                                                //check if the start of the object will become consumed by the addr change
                                                && (Short.toUnsignedInt(d.getStartAddress()) >= Short.toUnsignedInt(selectedDisplay.get().getStartAddress())
                                                && Short.toUnsignedInt(d.getStartAddress()) <= Short.toUnsignedInt((short)(selectedDisplay.get().getStartAddress() + 511)))) {
                                            isAddrTaken = true;
                                            break;
                                        }
                                    }

                                    if (isAddrTaken) {
                                        Alert a = new Alert(Alert.AlertType.ERROR);
                                        a.setTitle("Wait A Minute!");
                                        a.setHeaderText("Changing to this mode will make the display address space intrude into other devices' address space");
                                        a.showAndWait();
                                        justSwitchedDisplay.set(true);
                                        modeCB.getSelectionModel().select(oldMode);
                                        justSwitchedDisplay.set(false);
                                    } else {
                                        selectedDisplay.get().setMode64BW();
                                        updateDebuggerTA();
                                        DispButton.fire();
                                    }
                                }
                            } else if (modeCB.getSelectionModel().getSelectedIndex() == 1) {
                                if (Short.toUnsignedInt(selectedDisplay.get().getStartAddress()) > (65535 - 4095)) {
                                    //check if changing mode will go out of bounds
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Wait A Minute!");
                                    a.setHeaderText("Not sufficient Address Spaces left to change to this mode!");
                                    a.showAndWait();
                                    justSwitchedDisplay.set(true);
                                    modeCB.getSelectionModel().select(oldMode);
                                    justSwitchedDisplay.set(false);
                                } else {
                                    //check if changing mode intrudes into other's space
                                    boolean isAddrTaken = false;

                                    for (Device d : Bus.devices) {
                                        // check if proposed address is taken
                                        if (d != selectedDisplay.get()
                                                //check if the start of the object will become consumed by the addr change
                                                && (Short.toUnsignedInt(d.getStartAddress()) >= Short.toUnsignedInt(selectedDisplay.get().getStartAddress())
                                                && Short.toUnsignedInt(d.getStartAddress()) <= Short.toUnsignedInt((short)(selectedDisplay.get().getStartAddress() + 4095)))) {
                                            isAddrTaken = true;
                                            break;
                                        }
                                    }

                                    if (isAddrTaken) {
                                        Alert a = new Alert(Alert.AlertType.ERROR);
                                        a.setTitle("Wait A Minute!");
                                        a.setHeaderText("Changing to this mode will make the display address space intrude into other devices' address space");
                                        a.showAndWait();
                                        justSwitchedDisplay.set(true);
                                        modeCB.getSelectionModel().select(oldMode);
                                        justSwitchedDisplay.set(false);
                                    } else {
                                        selectedDisplay.get().setMode64RGB();
                                        updateDebuggerTA();
                                        DispButton.fire();
                                    }
                                }
                            }else if (modeCB.getSelectionModel().getSelectedIndex() == 2) {
                                if (Short.toUnsignedInt(selectedDisplay.get().getStartAddress()) > (65535 - 2047)) {
                                    //check if changing mode will go out of bounds
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Wait A Minute!");
                                    a.setHeaderText("Not sufficient Address Spaces left to change to this mode!");
                                    a.showAndWait();
                                    justSwitchedDisplay.set(true);
                                    modeCB.getSelectionModel().select(oldMode);
                                    justSwitchedDisplay.set(false);
                                } else {
                                    //check if changing mode intrudes into other's space
                                    boolean isAddrTaken = false;

                                    for (Device d : Bus.devices) {
                                        // check if proposed address is taken
                                        if (d != selectedDisplay.get()
                                                //check if the start of the object will become consumed by the addr change
                                                && (Short.toUnsignedInt(d.getStartAddress()) >= Short.toUnsignedInt(selectedDisplay.get().getStartAddress())
                                                && Short.toUnsignedInt(d.getStartAddress()) <= Short.toUnsignedInt((short)(selectedDisplay.get().getStartAddress() + 2047)))) {
                                            isAddrTaken = true;
                                            break;
                                        }
                                    }

                                    if (isAddrTaken) {
                                        Alert a = new Alert(Alert.AlertType.ERROR);
                                        a.setTitle("Wait A Minute!");
                                        a.setHeaderText("Changing to this mode will make the display address space intrude into other devices' address space");
                                        a.showAndWait();
                                        justSwitchedDisplay.set(true);
                                        modeCB.getSelectionModel().select(oldMode);
                                        justSwitchedDisplay.set(false);
                                    } else {
                                        selectedDisplay.get().setMode128BW();
                                        updateDebuggerTA();
                                        DispButton.fire();
                                    }
                                }
                            } else if (modeCB.getSelectionModel().getSelectedIndex() == 3) {
                                if (Short.toUnsignedInt(selectedDisplay.get().getStartAddress()) > (65535 - 16383)) {
                                    //check if changing mode will go out of bounds
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Wait A Minute!");
                                    a.setHeaderText("Not sufficient Address Spaces left to change to this mode!");
                                    a.showAndWait();
                                    justSwitchedDisplay.set(true);
                                    modeCB.getSelectionModel().select(oldMode);
                                    justSwitchedDisplay.set(false);
                                } else {
                                    //check if changing mode intrudes into other's space
                                    boolean isAddrTaken = false;

                                    for (Device d : Bus.devices) {
                                        // check if proposed address is taken
                                        if (d != selectedDisplay.get()
                                                //check if the start of the object will become consumed by the addr change
                                                && (Short.toUnsignedInt(d.getStartAddress()) >= Short.toUnsignedInt(selectedDisplay.get().getStartAddress())
                                                && Short.toUnsignedInt(d.getStartAddress()) <= Short.toUnsignedInt((short)(selectedDisplay.get().getStartAddress() + 16383)))) {
                                            isAddrTaken = true;
                                            break;
                                        }
                                    }

                                    if (isAddrTaken) {
                                        Alert a = new Alert(Alert.AlertType.ERROR);
                                        a.setTitle("Wait A Minute!");
                                        a.setHeaderText("Changing to this mode will make the display address space intrude into other devices' address space");
                                        a.showAndWait();
                                        justSwitchedDisplay.set(true);
                                        modeCB.getSelectionModel().select(oldMode);
                                        justSwitchedDisplay.set(false);
                                    } else {
                                        selectedDisplay.get().setMode128RGB();
                                        updateDebuggerTA();
                                        DispButton.fire();
                                    }
                                }
                            }
                        }
                        iv.setImage(selectedDisplay.get().getFrame());
                    }
            );


            this.clockCycleCount.textProperty().addListener((obs, oldV, newV) -> {
                if(selectedDisplay.get() != null) iv.setImage(selectedDisplay.get().getFrame());
                else iv.setImage(
                        new Image(Objects.requireNonNull(FrontControl.class
                                .getResourceAsStream("/FrontEnd/select display to view.png"))));
            });


            Button resetDispButt = new Button("Reset VRAM");
            resetDispButt.setOnAction(eh -> {
                if(selectedDisplay.get() != null) {
                    selectedDisplay.get().clearDisp();
                    iv.setImage(selectedDisplay.get().getFrame());
                }
                else iv.setImage(
                        new Image(Objects.requireNonNull(FrontControl.class
                                .getResourceAsStream("/FrontEnd/select display to view.png"))));
            });


            VBox returnedPane = new VBox();

            Label menuLabel = new Label("Display");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);
            returnedPane.getChildren().add(DisplayCB);
            returnedPane.getChildren().add(modeCB);
            returnedPane.getChildren().add(iv);
            returnedPane.getChildren().add(resetDispButt);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #3465a4; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

/*
    //button handler for scrapped sound emulation
    @FXML
    protected void onSoundButtonClick(){
        DeviceController dc = () -> {
            VBox returnedPane = new VBox();

            Label menuLabel = new Label("Sound (hopefully coming)");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #069a2e; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }
 */

    @FXML
    protected void onAllButtonClick(){
        DeviceController dc = () -> {
            //sort the Bus.Devices observable list
            Bus.devices.sort(new DeviceComparator());

            VBox returnedPane = new VBox();

            ScrollPane listOfDevicesSP = new ScrollPane();

            VBox insideVBox = new VBox();
            insideVBox.setStyle("-fx-alignment: top-center; -fx-spacing: 5; -fx-background-color: white;");

            for(Device d : Bus.devices){
                GridPane deviceGP = new GridPane();

                Label deviceName = new Label(d.getDeviceName());
                deviceName.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 14; -fx-font-weight: bold; -fx-wrap-text: true");
                GridPane.setColumnIndex(deviceName,0); GridPane.setRowIndex(deviceName,0);
                GridPane.setHgrow(deviceName, Priority.ALWAYS);
                GridPane.setHalignment(deviceName, HPos.CENTER);

                TextField startAddrTF = new TextField();
                if(d.getClass().getSimpleName().equals("Input") ){
                    startAddrTF.setDisable(true);
                }
                startAddrTF.setPrefWidth(100);
                startAddrTF.setText(String.format("0x%4s",Integer.toHexString(Short.toUnsignedInt(d.getStartAddress()))).replace(' ','0'));
                startAddrTF.setStyle("-fx-font-family: Monospaced; -fx-font-size: 13");
                GridPane.setColumnIndex(startAddrTF,1); GridPane.setRowIndex(startAddrTF,0);

                TextField endAddrTF = new TextField();
                if(d.getClass().getSimpleName().equals("Display")
                        || d.getClass().getSimpleName().equals("Input") ){
                    endAddrTF.setDisable(true);
                }
                endAddrTF.setPrefWidth(100);
                endAddrTF.setText(String.format("0x%4s",Integer.toHexString(Short.toUnsignedInt(d.getEndAddress()))).replace(' ','0'));
                endAddrTF.setStyle("-fx-font-family: Monospaced; -fx-font-size: 13");
                GridPane.setColumnIndex(endAddrTF,2); GridPane.setRowIndex(endAddrTF,0);

                Button deleteDeviceButt = new Button("Delete");
                deleteDeviceButt.setStyle("""
                -fx-font-family: Monospaced; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;
                -fx-background-color: maroon; -fx-border-color: red; -fx-border-width: 3""");
                GridPane.setColumnIndex(deleteDeviceButt, 2); GridPane.setRowIndex(deleteDeviceButt,1);
                GridPane.setHalignment(deleteDeviceButt, HPos.CENTER);

                Button editDeviceButt = new Button("Edit");
                GridPane.setColumnIndex(editDeviceButt, 1); GridPane.setRowIndex(editDeviceButt,1);
                GridPane.setHalignment(editDeviceButt, HPos.CENTER);
                editDeviceButt.setVisible(false);

                Tooltip DDBTooltip = new Tooltip("WARNING!\nTHIS WILL DELETE THE DEVICE \"" + d.getDeviceName() + "\"!");
                DDBTooltip.setStyle("-fx-background-color: red; -fx-text-alignment: center; -fx-font: bold 14 sans-serif");
                DDBTooltip.setShowDelay(Duration.millis(5));
                deleteDeviceButt.setTooltip(DDBTooltip);

                //delete device button handler
                deleteDeviceButt.setOnAction( eh-> {
                    if((Bus.devices.size() == 2 && Bus.devices.contains(inputObject))
                        || (Bus.devices.size() == 1 && !Bus.devices.contains(inputObject))){
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("BRUH!");
                        a.setHeaderText("You cannot have less than 1 device on the bus! (excluding input device)");
                        a.showAndWait();
                    } else {

                        if(d == inputObject){
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Too Bad!");
                            a.setHeaderText("Please remove input object from bus via input device's menu");
                            a.showAndWait();
                        } else {
                            Bus.devices.remove(d);
                            debuggerDropdown.getSelectionModel().select(Bus.devices.get(0));
                            AllButton.fire();
                        }
                    }


                });

                //editing
                //listen for changes in TFs
                startAddrTF.textProperty().addListener( (obs, oldVal, newVal) -> {
                    if(!editDeviceButt.isVisible()) editDeviceButt.setVisible(true);

                    //autoedit end address for fixed address size display objects based on new start address
                    String newStartAddrString = startAddrTF.getText().replaceFirst("0x", "");
                    try {
                        int newStartAddr = Integer.parseInt(newStartAddrString, 16);

                        if (d.getClass().getSimpleName().equals("Display")) {
                            endAddrTF.setText(String.format("0x%4s", Integer.toHexString(Short.toUnsignedInt((short) newStartAddr) + ((Display) d).getVRAMSize() - 1)).replace(' ', '0'));
                        }
                    } catch (NumberFormatException nfe){ /* ignored */ }
                });
                endAddrTF.textProperty().addListener( (obs, oldVal, newVal) -> {
                    if(!editDeviceButt.isVisible()) editDeviceButt.setVisible(true);
                });

                //on editing buttonpress
                editDeviceButt.setOnAction( eh -> {

                    String newStartAddrString = startAddrTF.getText().replaceFirst("0x", "");
                    String newEndAddrString = endAddrTF.getText().replaceFirst("0x", "");


                    //input object handler
                    if(d == inputObject){
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Unsupported Operation!");
                        a.setHeaderText("Please edit input object's parameters via input device's menu");
                        a.showAndWait();
                    }
                    // ram, rom and display device handler (mainly)
                    else {
                        try {

                            int newStartAddr = Integer.parseInt(newStartAddrString, 16);
                            int newEndAddr = Integer.parseInt(newEndAddrString, 16);

                            boolean isAddrTaken = false;

                            for (Device d1 : Bus.devices) {
                                // check if proposed address is taken
                                if (d1 != d
                                        && ((newStartAddr >= Short.toUnsignedInt(d1.getStartAddress())
                                        && newStartAddr <= Short.toUnsignedInt(d1.getEndAddress()))
                                        || (newEndAddr >= Short.toUnsignedInt(d1.getStartAddress())
                                        && newEndAddr <= Short.toUnsignedInt(d1.getEndAddress())))) {
                                    isAddrTaken = true;
                                    break;
                                }
                            }

                            if(!(Pattern.compile("[A-Fa-f0-9]{1,4}$").matcher(newStartAddrString).matches()
                                    && Pattern.compile("[A-Fa-f0-9]{1,4}$").matcher(newEndAddrString).matches())) {
                                //check input length
                                Alert a = new Alert(Alert.AlertType.ERROR);
                                a.setTitle("Bad Value!");
                                a.setHeaderText(currentTextRB.getString("badShortInputLengthErrorMsg"));
                                a.showAndWait();
                            } else if (newEndAddr < newStartAddr) {
                                // are you trying to put your end addr before new addr?
                                Alert a = new Alert(Alert.AlertType.ERROR);
                                a.setTitle("Bad Value!");
                                a.setHeaderText("Devices' end address cannot be in front of start address");
                                a.showAndWait();
                            } else if (isAddrTaken) {
                                // is new address space is already taken by other devices
                                Alert a = new Alert(Alert.AlertType.ERROR);
                                a.setTitle("Bad Value!");
                                a.setHeaderText("Proposed Address space is already taken!");
                                a.showAndWait();
                            } else {
                                // else set new address & update the Label
                                d.setStartAddress((short) newStartAddr);
                                d.setEndAddress((short) newEndAddr);
                                editDeviceButt.setVisible(false);
                            }
                        } catch (NumberFormatException nfe) {
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Bad Value!");
                            a.setHeaderText("values given for new address is bad!");
                            a.showAndWait();
                        }
                    }
                    //display new/old values
                    editDeviceButt.setVisible(false);
                    startAddrTF.setText(String.format("0x%4s",Integer.toHexString(Short.toUnsignedInt(d.getStartAddress()))).replace(' ','0'));
                    endAddrTF.setText(String.format("0x%4s",Integer.toHexString(Short.toUnsignedInt(d.getEndAddress()))).replace(' ','0'));
                    editDeviceButt.setVisible(false);
                    // refresh the choice box menu and debugger
                    Device currSel = debuggerLookAt;
                    Bus.devices.remove(d);
                    Bus.devices.add(d);
                    Bus.devices.sort(new DeviceComparator());
                    debuggerDropdown.getSelectionModel().select(currSel);
                    updateDebuggerTA();

                });

                deviceGP.getChildren().addAll(deviceName,startAddrTF,endAddrTF , deleteDeviceButt, editDeviceButt);

                if(d.getClass().getSimpleName().equals("ROM")) deviceGP.setStyle("-fx-background-color: #ea7500; -fx-border-color: #ff860d;");
                if (d.getClass().getSimpleName().equals("RAM")) deviceGP.setStyle("-fx-background-color: #d84315; -fx-border-color: #ff5429;");
                if (d.getClass().getSimpleName().equals("Input")) deviceGP.setStyle("-fx-background-color: #8d1d75; -fx-border-color: #780373;");
                if (d.getClass().getSimpleName().equals("Display")) deviceGP.setStyle("-fx-background-color: #5983b0; -fx-border-color: #3465a4;");
                deviceGP.setStyle(deviceGP.getStyle() +
                        "-fx-padding: 3; -fx-border-radius: 5; -fx-background-insets: 3; -fx-border-width: 3; -fx-hgap: 5; -fx-vgap: 5;");
                //deviceGP.setAlignment(Pos.CENTER);

                insideVBox.getChildren().add(deviceGP);
            }

            //make a pane to append to the end of insideVBox
            //it will create objects
            {
                GridPane addDeviceGP = new GridPane();
                addDeviceGP.setStyle(addDeviceGP.getStyle() +
                        "-fx-padding: 3; -fx-border-radius: 5; -fx-background-insets: 3; " +
                        "-fx-border-width: 3; -fx-hgap: 5; -fx-vgap: 5; " +
                        "-fx-background-color: black; -fx-border-color: #999999;");

                Label titleL = new Label("Create New Device");
                titleL.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 13.2; -fx-text-background-color: white");
                GridPane.setColumnIndex(titleL,0); GridPane.setRowIndex(titleL,0);
                GridPane.setColumnSpan(titleL, 2);

                TextField newDeviceNameTF = new TextField();
                newDeviceNameTF.setPromptText(currentTextRB.getString("newDeviceNameTFPrompt"));
                newDeviceNameTF.setStyle("-fx-font-family: Monospaced; -fx-font-size: 13;");
                GridPane.setColumnIndex(newDeviceNameTF,2); GridPane.setRowIndex(newDeviceNameTF,0);

                ChoiceBox<String> deviceTypeCB = new ChoiceBox<>(
                        FXCollections.observableArrayList(
                                "ROM", "RAM", "Display", "Cancel Creation"
                        )
                );
                Tooltip DTCBTooltip = new Tooltip("Select type of Device to add.");
                DTCBTooltip.setStyle("-fx-background-color: black; -fx-text-alignment: center; -fx-font: 13 sans-serif");
                DTCBTooltip.setShowDelay(Duration.millis(10));
                deviceTypeCB.setTooltip(DTCBTooltip);
                GridPane.setColumnIndex(deviceTypeCB,0); GridPane.setRowIndex(deviceTypeCB,1);

                TextField startAddrTF = new TextField();
                startAddrTF.setPromptText("start address");
                startAddrTF.setDisable(true);
                startAddrTF.setStyle("-fx-font-family: Monospaced; -fx-font-size: 13");
                GridPane.setColumnIndex(startAddrTF,1); GridPane.setRowIndex(startAddrTF,1);

                TextField endAddrTF = new TextField();
                endAddrTF.setPromptText("end address");
                endAddrTF.setDisable(true);
                endAddrTF.setStyle("-fx-font-family: Monospaced; -fx-font-size: 13");
                GridPane.setColumnIndex(endAddrTF,2); GridPane.setRowIndex(endAddrTF,1);

                Button addButton = new Button("Add Device");

                Button cancelCreationButt = new Button("Cancel");
                cancelCreationButt.setOnAction( eh -> deviceTypeCB.getSelectionModel().clearSelection());

                ArrayList<Node> additionalCreationNodes = new ArrayList<>();

                //on selecting what type to create
                deviceTypeCB.getSelectionModel().selectedItemProperty().addListener( (obs, oldval, newval) -> {
                    addDeviceGP.getChildren().clear();
                    additionalCreationNodes.clear();
                    if (newval == null){
                        //cancelled, disable both TFs
                        startAddrTF.clear(); endAddrTF.clear();
                        startAddrTF.setDisable(true); endAddrTF.setDisable(true);
                        //revert back to "blank slate menu"
                        addDeviceGP.getChildren().addAll(titleL, deviceTypeCB);
                        return;
                    } else if(newval.equals("ROM") || newval.equals("RAM")){
                        //if creating rom or ram
                        startAddrTF.clear(); endAddrTF.clear();
                        startAddrTF.setDisable(false); endAddrTF.setDisable(false);
                    } else if (newval.equals("Display")){
                        //if creating display
                        startAddrTF.clear(); endAddrTF.clear();
                        startAddrTF.setDisable(false); endAddrTF.setDisable(true);

                        // add custom extra settings CB for Display creation
                        ChoiceBox<String> newDispModeCB = new ChoiceBox<>(
                                FXCollections.observableArrayList(
                                        "64x64 1 bit BW",
                                        "64x64 6 bit RGB",
                                        "128x128 1 bit BW",
                                        "128x128 6 bit RGB"
                                )
                        );
                        newDispModeCB.getSelectionModel().select(0);
                        Tooltip NDMCBTooltip = new Tooltip("Select type of Display to add.");
                        NDMCBTooltip.setStyle("-fx-background-color: black; -fx-text-alignment: center; -fx-font: 13 sans-serif");
                        NDMCBTooltip.setShowDelay(Duration.millis(10));
                        newDispModeCB.setTooltip(NDMCBTooltip);

                        newDispModeCB.getSelectionModel().selectedIndexProperty().addListener((observableValue, NDMCBoldVal, NDMCBnewVal) -> {
                            String newStartAddrString = startAddrTF.getText().replaceFirst("0x", "");

                            try{
                                //autoedit end address for fixed address size display objects based on new start address

                                int newStartAddr = Integer.parseInt(newStartAddrString, 16);

                                int newDisplayVRAMSize;

                                // 512   -> 64BW
                                // 4096  -> 64RGB
                                // 2048  -> 128BW
                                // 16384 -> 128RGB
                                if(NDMCBnewVal.intValue() == 0) newDisplayVRAMSize = 512;
                                else if(NDMCBnewVal.intValue() == 1) newDisplayVRAMSize = 4096;
                                else if(NDMCBnewVal.intValue() == 2) newDisplayVRAMSize = 2048;
                                else newDisplayVRAMSize = 16384;

                                if(Short.toUnsignedInt((short) newStartAddr) + newDisplayVRAMSize - 1 > 65535){
                                    //check if new address will overflow
                                    endAddrTF.setText(currentTextRB.getString("error"));
                                } else endAddrTF.setText(String.format("0x%4s", Integer.toHexString(Short.toUnsignedInt((short) newStartAddr) + newDisplayVRAMSize - 1)).replace(' ', '0'));

                            } catch (NumberFormatException nfe) {
                                endAddrTF.setText(currentTextRB.getString("error"));
                            }
                        });

                        // add event handlers to the start addr TF to update end addr TF
                        startAddrTF.textProperty().addListener( (observableValue, oldVal, newVal) -> {
                            //autoedit end address for fixed address size display objects based on new start address
                            if (deviceTypeCB.getSelectionModel().getSelectedItem() != null
                                    && deviceTypeCB.getSelectionModel().getSelectedItem().equals("Display")) {
                                String newStartAddrString = startAddrTF.getText().replaceFirst("0x", "");
                                try {
                                    int newStartAddr = Integer.parseInt(newStartAddrString, 16);

                                    int newDisplayVRAMSize;

                                    // 512   -> 64BW
                                    // 4096  -> 64RGB
                                    // 2048  -> 128BW
                                    // 16384 -> 128RGB
                                    if(newDispModeCB.getSelectionModel().getSelectedIndex() == 0) newDisplayVRAMSize = 512;
                                    else if(newDispModeCB.getSelectionModel().getSelectedIndex() == 1) newDisplayVRAMSize = 4096;
                                    else if(newDispModeCB.getSelectionModel().getSelectedIndex() == 2) newDisplayVRAMSize = 2048;
                                    else newDisplayVRAMSize = 16384;

                                    if(Short.toUnsignedInt((short) newStartAddr) + newDisplayVRAMSize - 1 > 65535){
                                        //check if new address will overflow
                                        endAddrTF.setText(currentTextRB.getString("error"));
                                    } else endAddrTF.setText(String.format("0x%4s", Integer.toHexString(Short.toUnsignedInt((short) newStartAddr) + newDisplayVRAMSize - 1)).replace(' ', '0'));

                                } catch (NumberFormatException nfe) {
                                    endAddrTF.setText(currentTextRB.getString("error"));
                                }
                            }
                        });

                        additionalCreationNodes.add(newDispModeCB);
                    } else {
                        //catchall (reverts to "blank slate" menu
                        deviceTypeCB.getSelectionModel().clearSelection();
                        return;
                    }

                    // refresh device creation pane to show device creation params
                    addDeviceGP.getChildren().clear();
                    addDeviceGP.getChildren().addAll(titleL, newDeviceNameTF, deviceTypeCB, startAddrTF, endAddrTF);

                    int additionalNodeNum;
                    for (additionalNodeNum = 0; additionalNodeNum < additionalCreationNodes.size(); additionalNodeNum++) {
                        Node additionalSettingNode = additionalCreationNodes.get(additionalNodeNum);
                        GridPane.setColumnIndex(additionalSettingNode, additionalNodeNum % 3);
                        GridPane.setRowIndex(additionalSettingNode, 2 + (additionalNodeNum / 3));

                        addDeviceGP.getChildren().add(additionalSettingNode);
                    }
                    additionalNodeNum += 2;

                    GridPane.setColumnIndex(addButton, 1);
                    GridPane.setRowIndex(addButton, 2 + (additionalNodeNum / 3));

                    GridPane.setColumnIndex(cancelCreationButt, 2);
                    GridPane.setRowIndex(cancelCreationButt, 2 + (additionalNodeNum / 3));

                    addDeviceGP.getChildren().addAll(addButton, cancelCreationButt);
                });

                //adding functionality
                addButton.setOnAction( eh -> {
                    String newStartAddrString = startAddrTF.getText().replaceFirst("0x", "");
                    String newEndAddrString = endAddrTF.getText().replaceFirst("0x", "");

                    try {

                        int newStartAddr = Integer.parseInt(newStartAddrString, 16);
                        int newEndAddr = Integer.parseInt(newEndAddrString, 16);

                        boolean isAddrTaken = false;

                        for (Device d1 : Bus.devices) {
                            // check if proposed address is taken
                            if (((newStartAddr >= Short.toUnsignedInt(d1.getStartAddress())
                                    && newStartAddr <= Short.toUnsignedInt(d1.getEndAddress()))
                                    || (newEndAddr >= Short.toUnsignedInt(d1.getStartAddress())
                                    && newEndAddr <= Short.toUnsignedInt(d1.getEndAddress())))) {
                                isAddrTaken = true;
                                break;
                            }
                        }

                        if(!(Pattern.compile("[A-Fa-f0-9]{1,4}$").matcher(newStartAddrString).matches()
                                && Pattern.compile("[A-Fa-f0-9]{1,4}$").matcher(newEndAddrString).matches())) {
                            //check input length
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Bad Value!");
                            a.setHeaderText(currentTextRB.getString("badShortInputLengthErrorMsg"));
                            a.showAndWait();
                        } else if (newEndAddr < newStartAddr) {
                            // are you trying to put your end addr before new addr?
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Bad Value!");
                            a.setHeaderText("Devices' end address cannot be in front of start address");
                            a.showAndWait();
                        } else if (isAddrTaken) {
                            // is new address space is already taken by other devices
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setTitle("Bad Value!");
                            a.setHeaderText("Proposed Address space is already taken!");
                            a.showAndWait();
                        } else {
                            // initiate adding
                            if(deviceTypeCB.getSelectionModel().getSelectedIndex() == 0){
                                //add rom
                                Bus.devices.add(new ROM(newDeviceNameTF.getText(),(short) newStartAddr,(short) newEndAddr));
                            } else if(deviceTypeCB.getSelectionModel().getSelectedIndex() == 1){
                                //add ram
                                Bus.devices.add(new RAM(newDeviceNameTF.getText(),(short) newStartAddr,(short) newEndAddr));
                            } else if(deviceTypeCB.getSelectionModel().getSelectedIndex() == 2){
                                //add display
                                Display displayToAdd = new Display(newDeviceNameTF.getText(), (short) newStartAddr);
                                // 512   -> 64BW
                                // 4096  -> 64RGB
                                // 2048  -> 128BW
                                // 16384 -> 128RGB
                                if (newEndAddr - newStartAddr == 511) displayToAdd.setMode64BW();
                                else if (newEndAddr - newStartAddr == 4095) displayToAdd.setMode64RGB();
                                else if (newEndAddr - newStartAddr == 2048) displayToAdd.setMode128BW();
                                else if (newEndAddr - newStartAddr == 16383) displayToAdd.setMode128RGB();
                                Bus.devices.add(displayToAdd);
                            }
                            // refresh the choice box menu and debugger and sort
                            Device currSel = debuggerLookAt;
                            Bus.devices.sort(new DeviceComparator());
                            debuggerDropdown.getSelectionModel().select(currSel);
                            updateDebuggerTA();
                            //restart the all pane
                            AllButton.fire();
                        }
                    } catch (NumberFormatException nfe) {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Bad Value!");
                        a.setHeaderText("values given for new address is bad!");
                        a.showAndWait();
                    }
                });

                // add "blank slate" to pane
                addDeviceGP.getChildren().addAll(titleL, deviceTypeCB);

                ColumnConstraints colConstraints = new ColumnConstraints();
                colConstraints.setPercentWidth(100.0/2.75);
                colConstraints.setFillWidth(true);
                colConstraints.setHalignment(HPos.CENTER);
                addDeviceGP.getColumnConstraints().add(colConstraints);

                addDeviceGP.prefWidthProperty().bind(insideVBox.widthProperty());

                insideVBox.getChildren().add(addDeviceGP);
            }

            Label menuLabel = new Label("All Devices");
            menuLabel.setStyle("-fx-text-alignment: center; -fx-font-family: Monospaced; -fx-font-size: 18; -fx-text-background-color: white");
            returnedPane.getChildren().add(menuLabel);

            listOfDevicesSP.setContent(insideVBox);
            insideVBox.prefWidthProperty().bind(listOfDevicesSP.widthProperty().subtract(25));
            listOfDevicesSP.prefHeight(devicePane.getHeight()-menuLabel.getHeight());
            listOfDevicesSP.setStyle("-fx-border-width: 3; -fx-border-color: #999999; -fx-alignment: center;");
            returnedPane.getChildren().add(listOfDevicesSP);

            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #999999; -fx-padding: 10; -fx-alignment: top-center; -fx-spacing: 5");
            returnedPane.setPrefHeight(devicePane.getHeight());

            return returnedPane;
        };
        devicePane.getChildren().clear();
        devicePane.getChildren().add(dc.drawDetailedMenu());
    }

    public void updateDebuggerTA(){
        //remember debugger scroll position
        double scrollPosition = debuggerTA.scrollTopProperty().doubleValue();
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
        //restore debugger scroll position
        debuggerTA.setScrollTop(scrollPosition);
    }

    protected void updateRegistersPanel(){
        byte a, x, y, sp;
        short pc;

        a = Bus.processor.getAccumulator();
        x = Bus.processor.getXReg();
        y = Bus.processor.getYReg();
        sp = Bus.processor.getStackPointer();
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
        //try to clock
        //if an illegal opcode is requested, reset the cpu and throw error
        try{ Bus.processor.clock(); } catch (UnsupportedOperationException uoe) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Illegal Opcode!");
            a.setHeaderText(uoe.getMessage());
            a.setContentText("The CPU will Reset");
            a.showAndWait();
            this.resetCPUButtonAction();
        }
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
        debuggerDropdown.getSelectionModel().clearSelection();
        updateDebuggerTA();
    }

    @FXML
    protected void autoClockButtonAction(){
        if(autoClockActive) {
            //interrupt the thread
            autoClockTimeline.stop();
            autoClockActive = false;
            //change button colour to red
            autoClockButt.setStyle("-fx-background-color: #cc0000; -fx-border-color:  #3465a4; " +
                    "-fx-border-width: 3; -fx-background-insets: 1; -fx-border-radius: 5");
        }
        else{
            try  {
                //try to get Hz Double
                double autoClockHzIn = Double.parseDouble(autoClockTF.getText());

                //if 0Hz, do nothing (or else div by 0 error
                if(autoClockHzIn == 0) return;

                //clear old timeline of autoclock
                autoClockTimeline.getKeyFrames().clear();
                //set up a new timeline for the autoclock
                autoClockTimeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(1/autoClockHzIn), (ActionEvent event) -> this.stepClockOnAction())
                );

                autoClockTimeline.setCycleCount(Timeline.INDEFINITE);
                //start the thread
                autoClockTimeline.play();
                autoClockActive = true;
                //change button colour to green
                autoClockButt.setStyle("-fx-background-color: #00cc00; -fx-border-color:  #3465a4; " +
                        "-fx-border-width: 3; -fx-background-insets: 1; -fx-border-radius: 5");
            } catch (NumberFormatException nfe){
                //if bad (non-double) value was passed into CPU Hz TextField
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Not a valid CPU Hz!");
                a.setHeaderText("You did not enter a double for the autoclock CPU Hz parameter!");
                a.showAndWait();
            }
        }
    }

    @FXML
    protected void aboutMeClicked(){
        try{
            Stage aboutStage = new Stage();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("about.fxml")));

            //find the credits label
            final AtomicReference<Label> creditsLabel = new AtomicReference<>();
            for (Node n: root.getChildrenUnmodifiable()) {
                if(n.getClass().getSimpleName().equals("Label")) {
                    creditsLabel.set((Label) n);
                    creditsLabel.get().setText(currentTextRB.getString("aboutMeText"));
                }
            }

            //find the language buttons
            Button enButt = new Button();
            Button frButt = new Button();
            Button itButt = new Button();
            Button deButt = new Button();
            for (Node n: root.getChildrenUnmodifiable()) {
                if(n.getClass().getSimpleName().equals("Button")
                        && ((Button) n).getText().equals("EN")) enButt = (Button) n;

                if(n.getClass().getSimpleName().equals("Button")
                    && ((Button) n).getText().equals("FR")) frButt = (Button) n;

                if(n.getClass().getSimpleName().equals("Button")
                        && ((Button) n).getText().equals("IT")) itButt = (Button) n;

                if(n.getClass().getSimpleName().equals("Button")
                        && ((Button) n).getText().equals("DE")) deButt = (Button) n;
            }
            enButt.setOnAction( eh -> {
                changeLanguage(Locale.ENGLISH);
                if(creditsLabel.get() != null) {
                    creditsLabel.get().setText(currentTextRB.getString("aboutMeText"));
                }
            });
            frButt.setOnAction( eh -> {
                changeLanguage(Locale.FRANCE);
                if(creditsLabel.get() != null) {
                    creditsLabel.get().setText(currentTextRB.getString("aboutMeText"));
                }
            });
            itButt.setOnAction( eh -> {
                changeLanguage(Locale.ITALY);
                if(creditsLabel.get() != null) {
                    creditsLabel.get().setText(currentTextRB.getString("aboutMeText"));
                }
            });
            deButt.setOnAction( eh -> {
                changeLanguage(Locale.GERMANY);
                if(creditsLabel.get() != null) {
                    creditsLabel.get().setText(currentTextRB.getString("aboutMeText"));
                }
            });



            aboutStage.setScene(new Scene(root));
            aboutStage.setTitle("About Mii");
            aboutStage.initModality(Modality.WINDOW_MODAL);
            aboutStage.initOwner(allPane.getScene().getWindow());
            aboutStage.setResizable(false);
            aboutStage.show();
        } catch (Exception e){e.printStackTrace();}
    }

    private void changeLanguage(Locale l){
        ResourceBundle.clearCache();
        currentTextRB = ResourceBundle.getBundle("words", l);
    }

}