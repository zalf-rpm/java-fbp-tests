package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

@ComponentDescription("Simply consume values doing nothing.")
@InPort("IN")
public class Sink extends Component {
    InputPort inPort;

    @Override
    protected void execute() {
        Packet p;
        while ((p = inPort.receive()) != null)
            drop(p);
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
    }

}
