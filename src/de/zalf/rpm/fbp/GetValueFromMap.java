package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.List;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Get a value in a MAP under the given path (which may contain indices for lists).")
@InPorts({
        @InPort(value = "PATH", description = "Path to value in possibly nested maps", type = PVector.class),
        @InPort(value = "IN", description = "Map representing JSON Object", type = PCollection.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The requested value"),
        @OutPort(value = "PASS", description = "The original MAP passed through", type = PCollection.class, optional = true),
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

    PVector<SorI> path;
    int pathLevel = 0;

    PCollection coll;
    int collLevel = 0;

    @Override
    protected void execute() {

        //receive new path if possible
        if(pathLevel >= collLevel || path.isEmpty()) {
            Packet pp = pathPort.receive();
            if (pp == null)
                return;

            if(pp.getType() == Packet.OPEN){
                pathLevel++;
                drop(pp);
                pp = pathPort.receive();
                if(pp == null)
                    return;
            } else if(pp.getType() == Packet.CLOSE){
                pathLevel--;
                drop(pp);
                pp = pathPort.receive();
                if(pp == null)
                    return;
            }

            PVector p = (PVector) pp.getContent();
            drop(pp);
            path = Empty.vector();
            for (Object o : p) {
                if(o instanceof String)
                    path = path.plus(new SorI((String)o));
                else if(o instanceof Integer)
                    path = path.plus(new SorI((Integer)o));
            }
        }

        //receive new coll if possible
        if(collLevel >= pathLevel || coll == null) {
            //try to read a coll packet
            Packet mp = inPort.receive();
            if (mp == null)
                return;

            if (mp.getType() == Packet.OPEN) {
                collLevel++;
                drop(mp);
                mp = inPort.receive();
                if (mp == null)
                    return;
            } else if (mp.getType() == Packet.CLOSE) {
                collLevel--;
                drop(mp);
                mp = inPort.receive();
                if (mp == null)
                    return;
            }

            coll = (PCollection) mp.getContent();
            drop(mp);
        }

        Object o  = coll;
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
            passPort.send(create(coll));

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
