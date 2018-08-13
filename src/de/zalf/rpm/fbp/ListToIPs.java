package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;
import org.pcollections.Empty;

import java.util.List;

@ComponentDescription("Split the Lists on IN into IPs")
@InPorts({
        @InPort(value = "SSL", description = "Create SSL number of substream levels, -1 = default",
                type = Integer.class, optional = true),
        @InPort(value = "IN", description = "A possibly nested List of objects", type = List.class),
})
        @OutPort(value = "OUT", description = "IPs possibly nested up to level SSL into substream brackets")
public class ListToIPs extends Component {
    InputPort inPort;
    InputPort sslPort;
    OutputPort outPort;

    int ssl = -1;

    private void sendElements(List list, int level){
        if(level <= ssl)
            outPort.send(create(Packet.OPEN, ""+level));
        for(Object obj : list){
            if(List.class.isAssignableFrom(obj.getClass()) && level < ssl)
                sendElements((List)obj, level+1);
            else
                outPort.send(create(obj));
        }
        if(level <= ssl)
            outPort.send(create(Packet.CLOSE, ""+level));
    }

    @Override
    protected void execute() {
        Packet lp = sslPort.receive();
        if(lp != null){
            Object o = lp.getContent();
            if(o instanceof String)
                ssl = Integer.parseInt((String)o);
            else
                ssl = (Integer)lp.getContent();
            drop(lp);
        }

        Packet ip = inPort.receive();
        if(ip != null){
            List l = (List)ip.getContent();
            drop(ip);
            sendElements(l, 0);
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        sslPort = openInput("SSL");
        outPort = openOutput("OUT");
    }
}
