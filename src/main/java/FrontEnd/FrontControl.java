package FrontEnd;

import Devices.*;
import MainComComponents.Bus;
import MainComComponents.CPUFlags;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
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
            //if keypress has not yet been registered
            if (!inputObject.isKeyPressRegistered()){
                //if all characters are to be detected OR
                //if the keypress is in the allowed characters list
                if (inputObject.getAllowedCharacters() == null
                        || inputObject.getAllowedCharacters().contains(eh.getCode().getChar().charAt(0))){
                    inputObject.registerKeyPress(eh.getCode());

                    //if we should send a IRQ for this keypress,
                    if(inputObject.isSendKeyPressInterrupts()){
                        Bus.processor.IRQ();
                    }
                }
                inputObject.setKeyPressRegistered(true);
                //update the debug pane if Input item is on the bus
                if(Bus.devices.contains(inputObject)) updateDebuggerTA();
            }
        });
        allPane.setOnKeyReleased( eh -> {
            if(!inputObject.isStickyKeys()){
                //reset to 0x0000 using key 0x0000
                inputObject.clearKey();
            }
            //open for new keypress
            inputObject.setKeyPressRegistered(false);
            //update the debug pane if Input item is on the bus
            if(Bus.devices.contains(inputObject)) updateDebuggerTA();
        });


    }

    @FXML
    protected void onROMButtonClick(){
        DeviceController dc = () -> {
            AtomicReference<ROM> selectedROM = new AtomicReference<>();

            ChoiceBox<ROM> ROMCB = new ChoiceBox<>();
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

            returnedPane.getChildren().add(new Label("ROM Flasher"));

            returnedPane.getChildren().add(ROMCB);
            returnedPane.getChildren().add(BINfilepathTF);
            returnedPane.getChildren().add(writeROMButt);
            returnedPane.getChildren().add(wipeROMButt);

            returnedPane.setAlignment(Pos.CENTER);
            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color:  #ff860d; -fx-padding: 10;");

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
                    } while (currAddr != debuggerLookAt.getEndAddress());
                } else {
                    sb.append("Select_A_RAM_Device_to_view.");
                }
                RAMDisp.setText(sb.toString().replace(' ','0').replace('_',' '));
                //restore ram disp scroll position
                RAMDisp.setScrollTop(scrollPosition);
            });

            ChoiceBox<RAM> RAMCB = new ChoiceBox<>();
            RAMCB.setPrefHeight(new Text("RAM\nRAM").getLayoutBounds().getHeight() + 10);
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
                            } while (currAddr != debuggerLookAt.getEndAddress());
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
                    } while (currAddr != debuggerLookAt.getEndAddress());
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

            returnedPane.getChildren().add(new Label("RAM Viewer"));
            returnedPane.getChildren().add(RAMCB);
            returnedPane.getChildren().add(RAMDisp);
            returnedPane.getChildren().add(resetButt);

            returnedPane.setAlignment(Pos.CENTER);
            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #ff5429; -fx-padding: 10;");

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
            {
                String startAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getStartAddress()));
                String endAddrString = Integer.toHexString(Short.toUnsignedInt(inputObject.getEndAddress()));

                currentAddrL.setText(String.format("Current_Address_:_0x%4s_0x%4s",startAddrString,endAddrString)
                        .replace(' ','0').replace('_',' '));
            }


            TextField addressTF = new TextField();
            addressTF.setPromptText("short new address (hex value)");

            ToolBar addrEditorButts = new ToolBar();

            Button addressButt = new Button("Set New Address");

            addressButt.setOnAction( eh -> {
                short editAddr;
                try{
                    editAddr = (short)Integer.parseInt(addressTF.getText(),16);

                    boolean isAddrTaken = false;

                    for(Device d : Bus.devices){
                        // check if proposed address is taken
                        if((Short.toUnsignedInt(editAddr) >= d.getStartAddress()
                                && Short.toUnsignedInt(editAddr) <= d.getEndAddress())
                        || (Short.toUnsignedInt((short) (editAddr+1)) >= d.getStartAddress()
                                && Short.toUnsignedInt((short) (editAddr+1)) <= d.getEndAddress())) {
                            isAddrTaken = true; break;
                        }
                    }

                    if(isAddrTaken){
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

                        currentAddrL.setText(String.format("Current_Address_:_0x%4s_0x%4s",startAddrString,endAddrString)
                                .replace(' ','0').replace('_',' '));
                    }

                } catch (NumberFormatException nfe) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Bad Value!");
                    a.setHeaderText("values given for new address is bad!");
                    a.showAndWait();
                }
                // TODO: 21/9/2022 if the current object is inputObject, refresh the choice box by plugging in and out inputObject from bus
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
                        if((Short.toUnsignedInt(inputObject.getStartAddress()) >= d.getStartAddress()
                                && Short.toUnsignedInt(inputObject.getStartAddress()) <= d.getEndAddress())
                                || (Short.toUnsignedInt(inputObject.getEndAddress()) >= d.getStartAddress()
                                && Short.toUnsignedInt(inputObject.getEndAddress()) <= d.getEndAddress())) {
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
                    if(debuggerLookAt == inputObject)
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
            setCharsToDetectButt.setStyle("-fx-border-color:  blue;");

            Button detectAllButt = new Button("Detect All Characters");

            setCharsToDetectButt.setOnAction( eh -> {
                inputObject.setAllowedCharacters(
                        (detectCharsTF.getText().toLowerCase() + detectCharsTF.getText().toUpperCase()).toCharArray());
                setCharsToDetectButt.setStyle("-fx-border-color:  blue;");
                detectAllButt.setStyle("-fx-border-color:  none;");
            });
            detectAllButt.setOnAction( eh -> {
                inputObject.allAllowedCharacters();
                detectAllButt.setStyle("-fx-border-color:  blue;");
                setCharsToDetectButt.setStyle("-fx-border-color:  none;");
            });

            if(inputObject.getAllowedCharacters() == null) {
                detectAllButt.setStyle("-fx-border-color:  blue;");
                setCharsToDetectButt.setStyle("-fx-border-color:  none;");
            }

            detectCharsButts.getItems().add(setCharsToDetectButt);
            detectCharsButts.getItems().add(detectAllButt);
            detectCharsButts.setStyle("-fx-alignment: center; -fx-background-color: none;");


            ToolBar otherInputSettingsButts= new ToolBar();

            ToggleButton toSendIRQTB = new ToggleButton("send IRQ on keypress?");
            if(inputObject.isSendKeyPressInterrupts()) toSendIRQTB.setSelected(true);
            toSendIRQTB.setOnAction(eh -> {
                inputObject.setSendKeyPressInterrupts(toSendIRQTB.isSelected());
            });

            ToggleButton stickyKeysTB = new ToggleButton("StickyKeys");
            if(inputObject.isStickyKeys()) stickyKeysTB.setSelected(true);
            Tooltip stickyKeysTT = new Tooltip("if selected, keycode will not get cleared when key is released.\n" +
                    "if not selected, keycode will reset to 0x0000 on release");
            stickyKeysTT.setStyle("-fx-font: 12 sans-serif");
            stickyKeysTB.setTooltip(stickyKeysTT);
            stickyKeysTB.setOnAction(eh -> {
                inputObject.setStickyKeys(stickyKeysTB.isSelected());
            });

            otherInputSettingsButts.getItems().add(toSendIRQTB);
            otherInputSettingsButts.getItems().add(stickyKeysTB);
            otherInputSettingsButts.setStyle("-fx-alignment: center; -fx-background-color: none;");


            returnedPane.getChildren().add(new Label("Input Object (you only get one)"));
            returnedPane.getChildren().add(currentAddrL);
            returnedPane.getChildren().add(addressTF);
            returnedPane.getChildren().add(addrEditorButts);
            returnedPane.getChildren().add(detectCharsTF);
            returnedPane.getChildren().add(detectCharsButts);
            returnedPane.getChildren().add(otherInputSettingsButts);

            returnedPane.setAlignment(Pos.CENTER);
            returnedPane.setStyle("-fx-border-width: 3; -fx-border-color: #780373; -fx-padding: 10;");

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

}