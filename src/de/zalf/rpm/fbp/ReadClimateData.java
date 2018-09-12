package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

@ComponentDescription("Create a PMap representing the JSON serialization of the read climate data")
@InPorts({
        @InPort(value = "OPT", description = "JSON string with climate data options", type = String.class, optional = true),
        @InPort(value = "IN", description = "CSV formatted climate data string", type = String.class)
})
@OutPort(value = "OUT", description = "String with JSON representation of climate data", type = String.class)
public class ReadClimateData extends Component {

    static {
        Native.register("libclimateio");
    }

    public static native Pointer Climate_readClimateDataFromCSVStringViaHeaders(String csvString, String options);
    public static native void Climate_freeCString(Pointer str);

    private InputPort inPort;
    private InputPort optPort;
    private OutputPort outPort;

    private String jsonOptsStr;

    @Override
    protected void execute() {
        if(!optPort.isClosed()){
            Packet op = optPort.receive();
            if (op == null)
                return;

            jsonOptsStr = (String)op.getContent();
            drop(op);
            optPort.close();
        }

        Packet p;
        while ((p  = inPort.receive()) != null) {
            String csvStr = (String)p.getContent();
            drop(p);

            Pointer jsonPtr = Climate_readClimateDataFromCSVStringViaHeaders(csvStr, jsonOptsStr);
            if(jsonPtr == null)
                continue;
            String json = jsonPtr.getString(0);
            Climate_freeCString(jsonPtr);

            outPort.send(create(json));
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        optPort = openInput("OPT");
        outPort = openOutput("OUT");
    }
}
