package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.List;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Get a value from the PMap or PVector under the given path (which may contain indices for lists).")
@InPorts({
        @InPort(value = "PATH", description = "Path to value in possibly nested maps", type = List.class),
        @InPort(value = "IN", description = "PMap or PVector representing JSON datastructure"),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The requested value"),
        @OutPort(value = "PASS", description = "The original PMap or PVector passed through", optional = true),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class GetValueFromPMapOrVec extends Component {
    InputPort inPort;
    InputPort pathPort;
    OutputPort outPort;
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

    Object origIn;
    int inLevel = 0;
    boolean sendInOpenBracket = false;

    @Override
    protected void execute() {

        //receive new path if possible
        if(pathLevel >= inLevel || path.isEmpty()) {
            Packet pp = pathPort.receive();
            if (pp == null)
                return;

            if(pp.getType() == Packet.OPEN){
                pathLevel++;
                drop(pp);
                sendInOpenBracket = true;
                pp = pathPort.receive();
                if(pp == null)
                    return;
            } else if(pp.getType() == Packet.CLOSE){
                pathLevel--;
                drop(pp);
                outPort.send(create(Packet.CLOSE, ""));
                pp = pathPort.receive();
                if(pp == null)
                    return;
            }

            List p = (List) pp.getContent();
            drop(pp);
            path = Empty.vector();
            for (Object o : p) {
                if(o instanceof String)
                    path = path.plus(new SorI((String)o));
                else if(o instanceof Integer)
                    path = path.plus(new SorI((Integer)o));
            }
        }

        //receive new origIn if possible
        if(inLevel >= pathLevel || origIn == null) {
            //try to read a origIn packet
            Packet mp = inPort.receive();
            if (mp == null)
                return;

            if (mp.getType() == Packet.OPEN) {
                inLevel++;
                drop(mp);
                mp = inPort.receive();
                if (mp == null)
                    return;
            } else if (mp.getType() == Packet.CLOSE) {
                inLevel--;
                drop(mp);
                mp = inPort.receive();
                if (mp == null)
                    return;
            }

            origIn = mp.getContent();
            drop(mp);
        }

        Object o  = origIn;
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

        if(sendInOpenBracket){
            outPort.send(create(Packet.OPEN, ""));
            sendInOpenBracket = false;
        }

        outPort.send(create(o));
        if(passPort.isConnected())
            passPort.send(create(origIn));

    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        pathPort = openInput("PATH");
        outPort = openOutput("OUT");
        passPort = openOutput("PASS");
        errorPort = openOutput("ERROR");
    }
}
