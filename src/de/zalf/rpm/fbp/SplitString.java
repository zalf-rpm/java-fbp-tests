package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.Arrays;
import java.util.List;

@ComponentDescription("Split a List by selecting elements and forwarding them in order to the array OUT")
@InPorts({
        @InPort(value = "AT", description = "regular expression where splitting the string happends"),
        @InPort(value = "IN", description = "the string to split", type = String.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "a list of the split parts of the string", type = List.class),
})
public class SplitString extends Component {
    private InputPort inPort;
    private InputPort atPort;
    private OutputPort outPort;

    private String splitAtRegex;

    @Override
    protected void execute() {
        if(!atPort.isClosed()){
            Packet sp = atPort.receive();
            if (sp == null)
                return;
            splitAtRegex = (String)sp.getContent();
            drop(sp);
            atPort.close();
        }

        Packet ip;
        while((ip = inPort.receive()) != null){
            String s = (String)ip.getContent();
            drop(ip);
            outPort.send(create(Arrays.asList(s.split(splitAtRegex))));
            //System.out.println(Thread.currentThread().getName() + ": sent on IN: " + s);
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        atPort = openInput("AT");
        outPort = openOutput("OUT");
    }
}
