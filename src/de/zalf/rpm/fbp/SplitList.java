package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.ArrayList;
import java.util.List;

@ComponentDescription("Split a List by selecting elements and forwarding them in order to the array OUT")
@InPorts({
        @InPort(value = "SEL", description = "List or String (separated by ,) with the selector keys"),
        @InPort(value = "IN", description = "List"),//, type = List.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The selected elements from the list in IN", arrayPort = true),
})

public class SplitList extends Component {
    InputPort inPort;
    InputPort selPort;
    OutputPort[] outPortArray;

    List<Integer> indices;

    int substreamLevel = 0;

    @Override
    protected void execute() {
        if(!selPort.isClosed()){
            Packet sp = selPort.receive();
            if (sp == null)
                return;

            Object sel = sp.getContent();
            drop(sp);
            selPort.close();

            if(sel instanceof String) {
                indices = new ArrayList<Integer>();
                for(String s : ((String) sel).split(",")) {
                    indices.add(Integer.parseInt(s.trim()));
                }
            }
            else if(List.class.isAssignableFrom(sel.getClass()))
                indices = (List<Integer>)sel;
        }

        Packet ip;
        while((ip = inPort.receive()) != null){
            if(ip.getType() != Packet.NORMAL){
                for(int i = 0; i < indices.size(); i++){
                    if(i < outPortArray.length)
                        outPortArray[i].send(create(ip.getType(), "" + substreamLevel));
                }
                drop(ip);
                continue;
            }

            List l = (List)ip.getContent();
            drop(ip);
            int size = l.size();

            for(int i = 0; i < indices.size(); i++){
                int index = indices.get(i);
                if(index < size && i < outPortArray.length)
                    outPortArray[i].send(create(l.get(index)));
            }
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        selPort = openInput("SEL");
        outPortArray = openOutputArray("OUT");
    }
}
