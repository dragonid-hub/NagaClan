package id.dragon.nagaclan.storage;

import id.dragon.nagaclan.model.Clan;

import java.io.IOException;

@FunctionalInterface
public interface ClanStorage {
    void save(Clan clan) throws IOException;

    default void delete(String clanId) throws IOException {
    }
}
