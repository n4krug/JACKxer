package space.n4krug.JACKxer.jackManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientRegistry {

    private Map<String, Client> clients = new HashMap<>();

    public void register(String name, Client client) {
        clients.put(name, client);
    }

    public Client get(String name) {
        return clients.get(name);
    }
    
    public Client getAnyClient() {
        return clients.values().iterator().next();
    }
    
    public Set<String> getClientNames() {
    	return clients.keySet();
    }
}