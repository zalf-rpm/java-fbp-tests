package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Attaches IP received on port ATTACH to IP received on port IN.")
@InPorts({
        @InPort(value = "ATTACH", description = "IP to attach"),
        @InPort(value = "IN", description = "IP to attach to"),
        @InPort(value = "KEY", description = "Key to use for attachment", type = String.class),
})
@OutPort(value = "OUT", description = "The IN IP with an attached IP from ATTACH under key KEY")
public class Attach extends Component {
    InputPort inPort;
    InputPort attachPort;
    InputPort keyPort;
    OutputPort outPort;

    @Override
    protected void execute() {
        Packet kp = keyPort.receive();
        if(kp == null)
            return;
        String key = (String)kp.getContent();
        drop(kp);

        Packet ap = attachPort.receive();
        if(ap == null)
            return;

        Packet ip = inPort.receive();
        if(ip == null)
            return;
        attach(ip, key, ap);

        outPort.send(ip);
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        attachPort = openInput("ATTACH");
        keyPort = openInput("KEY");
        outPort = openOutput("OUT");
    }
}
