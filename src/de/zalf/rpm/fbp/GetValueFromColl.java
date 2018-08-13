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
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class GetValueFromColl extends Component {
    private InputPort inPort;
    private InputPort pathPort;
    private OutputPort outPort;
    private OutputPort errorPort;

    private PVector path;
    private boolean receiveNextPath = true;
    private int pathLevel = 0;

    private Object coll;
    private boolean receiveNextColl = true;

    private void tryReceivingPath(){
        //allow receives only if a new path is requested
        if(receiveNextPath) {
            Packet pp;
            while((pp = pathPort.receive()) != null) {
                if(pp.getType() == Packet.OPEN){
                    pathLevel++;
                    drop(pp);
                    outPort.send(create(Packet.OPEN, ""+pathLevel));
                } else if(pp.getType() == Packet.CLOSE){
                    pathLevel--;
                    drop(pp);
                    outPort.send(create(Packet.CLOSE, ""+pathLevel));
                    receiveNextColl = true;
                    return;
                } else
                    break;
            }

            if(pp != null) {
                Object p = pp.getContent();
                if(p instanceof String)
                    path = Empty.vector().plus(p);
                else
                    path = TreePVector.from((List)pp.getContent());
                drop(pp);
                receiveNextPath = false;
            }
        }
    }

    private void tryReceivingColl(){
        if(receiveNextColl) {
            Packet cp;
            while((cp = inPort.receive()) != null) {
                if (cp.getType() == Packet.OPEN) {
                    if (errorPort.isConnected())
                        errorPort.send(create("(Open)-Brackets have no semantics for IN port! Ignoring open bracket."));
                    drop(cp);
                } else if (cp.getType() == Packet.CLOSE) {
                    if (errorPort.isConnected())
                        errorPort.send(create("(Close)-Brackets have no semantics for IN port! Ignoring close bracket."));
                    drop(cp);
                    return;
                } else
                    break;
            }

            if(cp != null) {
                coll = cp.getContent();
                drop(cp);
                receiveNextColl = false;
            }
        }
    }


    @Override
    protected void execute() {
        tryReceivingPath();
        tryReceivingColl();

        //if fields aren't yet initialized we can't continue
        if(path == null || coll == null)
            return;

        //to continue all values have to be in sync
        if(receiveNextPath || receiveNextColl)
            return;

        Object o  = coll;
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
                        o = coll;
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
                        o = coll;
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

        outPort.send(create(result ? o : ""));
        receiveNextPath = true;
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        pathPort = openInput("PATH");
        outPort = openOutput("OUT");
        errorPort = openOutput("ERROR");
    }
}
