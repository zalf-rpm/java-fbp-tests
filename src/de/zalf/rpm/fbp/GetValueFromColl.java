package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.List;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Get a value from the PMap or PVector under the given path (which may contain keys for lists).")
@InPorts({
        @InPort(value = "PATH", description = "Path List<Object> or single key (Object == Integer (index) or String) to value in possibly nested maps"),
        @InPort(value = "IN", description = "Map<String, Object> or List<Object>"),
})
@OutPorts({
        @OutPort(value = "OUT", description = "The requested value"),
        @OutPort(value = "PASS", description = "The original PMap or PVector passed through", optional = true),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class GetValueFromColl extends Component {
    InputPort inPort;
    InputPort pathPort;
    OutputPort outPort;
    OutputPort passPort;
    OutputPort errorPort;

    PVector path;
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

            Object p = pp.getContent();
            if(p instanceof String)
                path = Empty.vector().plus(p);
            else
                path = TreePVector.from((List) pp.getContent());
            drop(pp);
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
        boolean result = false;
        PVector prevPath = Empty.vector();
        for(Object k : path) {
            if(k instanceof Integer){
                int key = (Integer)k;
                if(List.class.isAssignableFrom(o.getClass())){
                    List l = (List)o;
                    if(key > 0 && key < l.size()) {
                        o = l.get(key);
                        result = true;
                    } else if(key == -1){
                        o = origIn;
                        result = true;
                    }
                    else if(errorPort.isConnected()) {
                        errorPort.send(create("Path [" + prevPath.plus(k) + "] doesn't designate a valid index. Waiting for next IP.!"));
                        return;
                    }
                } else if(errorPort.isConnected()) {
                    errorPort.send(create("Path [" + prevPath.plus(k) + "] doesn't designate a VECTOR/LIST. Waiting for next IP.!"));
                    return;
                }
            } else if(k instanceof String) {
                String key = (String)k;
                if(Map.class.isAssignableFrom(o.getClass())){
                    Map m = (Map)o;
                    if(m.containsKey(key)) {
                        o = m.get(key);
                        result = true;
                    } else if(key == "__all__"){
                        o = origIn;
                        result = true;
                    }
                    else if(errorPort.isConnected()) {
                        errorPort.send(create("Path [" + prevPath.plus(k) + "] doesn't designate a valid key in MAP. Waiting for next IP.!"));
                        return;
                    }
                } else if(errorPort.isConnected()) {
                    errorPort.send(create("Path [" + prevPath.plus(k) + "] doesn't designate a MAP. Waiting for next IP.!"));
                    return;
                }
            } else {
                return;
            }

            prevPath = prevPath.plus(k);
        }

        if(sendInOpenBracket){
            outPort.send(create(Packet.OPEN, ""));
            sendInOpenBracket = false;
        }

        outPort.send(create(result ? o : ""));
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
