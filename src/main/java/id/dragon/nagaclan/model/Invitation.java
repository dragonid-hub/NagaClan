package id.dragon.nagaclan.model;
import java.time.Instant;
public record Invitation(String clanId, Instant expiresAt) { public boolean expired(){return Instant.now().isAfter(expiresAt);} }
