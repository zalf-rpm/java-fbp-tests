package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ComponentDescription("Split a Map<String, Object> by selecting elements and forwarding them in order to the array OUT")
@InPorts({
        @InPort(value = "SEL", description = "List<String> or String (separated by ,) with the selector keys"),
        @InPort(value = "IN", description = "Map<String, Object>", type = Map.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The selected elements from the list in IN", arrayPort = true),
})

public class SplitMap extends Component {
    InputPort inPort;
    InputPort selPort;
    OutputPort[] outPortArray;

    List<String> keys;

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
                keys = new ArrayList<>();
                for(String s : ((String) sel).split(","))
                    keys.add(s.trim());
            }
            else if(List.class.isAssignableFrom(sel.getClass()))
                keys = (List<String>)sel;
        }

        Packet ip;
        while((ip = inPort.receive()) != null){
            Map<String, Object> m = (Map<String, Object>)ip.getContent();
            drop(ip);

            for(int i = 0; i < keys.size(); i++){
                String key = keys.get(i);
                if(m.containsKey(key) && i < outPortArray.length)
                    outPortArray[i].send(create(m.get(key)));
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
