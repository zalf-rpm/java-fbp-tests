package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@InPorts({
        @InPort(value = "IN", type = String.class),
        @InPort(value = "SEP", description = "separator between concatenated strings",
                optional = true, type = String.class)})
@OutPort(value = "OUT", type = String.class)
public class ConcatString extends Component {
    InputPort inPort;
    InputPort sepPort;
    OutputPort outPort;

    String separator = "";

    //ConcatString

    @Override
    protected void execute() {
        if (!sepPort.isClosed()) {
            Packet sepp = sepPort.receive();
            if(sepp == null)
                return;
            sepPort.close();

            separator = (String)sepp.getContent();
            drop(sepp);
        }

        String target = "";
        Packet p = inPort.receive();
        while (p != null) {
            String s = (String)p.getContent();
            target = target + s + separator;
            drop(p);
            p = inPort.receive();
        }
        Packet out = create(target);
        outPort.send(out);
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        sepPort = openInput("SEP");
        outPort = openOutput("OUT");
    }

}
