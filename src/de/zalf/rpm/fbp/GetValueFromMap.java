package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.List;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Get a value in a MAP under the given path (which may contain indices for lists).")
@InPorts({
        @InPort(value = "PATH", description = "Path (a string) to value in possibly nested maps", type = String.class),
        @InPort(value = "IN", description = "Map representing JSON Object", type = PMap.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The requested value"),
        @OutPort(value = "PASS", description = "The original MAP passed through", type = PMap.class, optional = true),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class GetValueFromMap extends Component {
    InputPort inPort;
    InputPort pathPort;
    OutputPort valuePort;
    OutputPort passPort;
    OutputPort errorPort;

    class SorI {
        SorI(String k){ key = k; }
        SorI(int i){ index = i; }
        String key;
        int index = -1;
        boolean isIndex() { return index >= 0; }
        boolean isStringKey() { return !isIndex(); }
        public String toString() { return index < 0 ? "" + index : key; }
    }

    String pathsIIP;
    PVector<SorI> path;

    @Override
    protected void execute() {
        //read IIP for path
        if (!pathPort.isClosed()) {
            Packet p = pathPort.receive();
            if (p == null)
                return;
            pathsIIP = (String)p.getContent();
            path = Empty.vector();
            for(String s : pathsIIP.split(",")) {
                String st = s.trim();
                try {
                    int i = Integer.parseInt(st);
                    path = path.plus(new SorI(i));
                } catch (NumberFormatException nfe){
                    path = path.plus(new SorI(st));
                }
            }
            drop(p);
            pathPort.close();
        }

        //try to read a map packet
        Packet mp = inPort.receive();
        if (mp != null) {
            PMap<String, Object> map  = (PMap<String, Object>)mp.getContent();
            Object o  = map;
            drop(mp);

            PVector<SorI> prevPath = Empty.vector();
            for(SorI key : path) {
                if(key.isIndex()){
                    if(List.class.isAssignableFrom(o.getClass())){
                        List l = (List)o;
                        if(key.index < l.size())
                            o = l.get(key.index);
                        else if(errorPort.isConnected()) {
                            errorPort.send(create("Path [" + prevPath.plus(key) + "] doesn't designate a valid index. Waiting for next IP.!"));
                            return;
                        }
                    } else if(errorPort.isConnected()) {
                        errorPort.send(create("Path [" + prevPath.plus(key) + "] doesn't designate a VECTOR/LIST. Waiting for next IP.!"));
                        return;
                    }
                } else {
                    if(Map.class.isAssignableFrom(o.getClass())){
                        Map m = (Map)o;
                        if(m.containsKey(key.key))
                            o = m.get(key.key);
                        else if(errorPort.isConnected()) {
                            errorPort.send(create("Path [" + prevPath.plus(key) + "] doesn't designate a valid key in MAP. Waiting for next IP.!"));
                            return;
                        }
                    } else if(errorPort.isConnected()) {
                        errorPort.send(create("Path [" + prevPath.plus(key) + "] doesn't designate a MAP. Waiting for next IP.!"));
                        return;
                    }
                }

                prevPath = prevPath.plus(key);
            }

            valuePort.send(create(o));
            if(passPort.isConnected())
                passPort.send(create(map));
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        pathPort = openInput("PATH");
        valuePort = openOutput("OUT");
        passPort = openOutput("PASS");
        errorPort = openOutput("ERROR");
    }
}
