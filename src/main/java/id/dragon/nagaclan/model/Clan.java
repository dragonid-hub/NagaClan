package id.dragon.nagaclan.model;
import id.dragon.nagaclan.text.Messages;

import java.util.*;

public final class Clan {
    private final String id, name;
    private final Map<UUID, ClanRole> members = new HashMap<>();
    private final Map<String, String> homes = new HashMap<>();
    private final Set<String> alliances = new HashSet<>();
    private final ClanSettings settings = new ClanSettings();
    private long bank; private int level = 1; private long experience;
    public Clan(String id, String name, UUID leader) { this.id=id; this.name=name; members.put(leader, ClanRole.LEADER); }
    public String id(){return id;} public String name(){return name;} public Map<UUID,ClanRole> members(){return Collections.unmodifiableMap(members);}
    public UUID leaderId(){
        UUID leader = null;
        for (Map.Entry<UUID, ClanRole> member : members.entrySet()) {
            if (member.getValue() != ClanRole.LEADER) continue;
            if (leader != null) throw new IllegalStateException("Clan " + id + " has multiple leaders");
            leader = member.getKey();
        }
        if (leader == null) throw new IllegalStateException("Clan " + id + " has no leader");
        return leader;
    }
    public ClanRole roleOf(UUID id){return members.get(id);} public boolean hasMember(UUID id){return members.containsKey(id);}
    public void addMember(UUID id, ClanRole role){members.put(id, role);} public void removeMember(UUID id){members.remove(id);}
    public void setRole(UUID id, ClanRole role){if(!members.containsKey(id)) throw new IllegalArgumentException(Messages.text("Bukan anggota")); members.put(id,role);}
    public Map<String,String> homes(){return homes;} public Set<String> alliances(){return alliances;} public ClanSettings settings(){return settings;}
    public long bank(){return bank;} public void setBank(long value){bank=Math.max(0,value);} public int level(){return level;} public long experience(){return experience;}
    public void restoreProgress(int level, long experience){this.level=Math.max(1,level);this.experience=Math.max(0,experience);}
    public void addExperience(long amount){experience=Math.max(0,experience+amount); while(experience>=level*1000L){experience-=level*1000L;level++;}}
}
