package org.maxgamer.maxbans.service;

import org.bukkit.entity.Player;
import org.maxgamer.maxbans.config.PluginConfig;
import org.maxgamer.maxbans.exception.RejectedException;
import org.maxgamer.maxbans.orm.Ban;
import org.maxgamer.maxbans.orm.Mute;
import org.maxgamer.maxbans.orm.User;
import org.maxgamer.maxbans.repository.BanRepository;
import org.maxgamer.maxbans.repository.MuteRepository;
import org.maxgamer.maxbans.repository.UserRepository;
import org.maxgamer.maxbans.util.RestrictionUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Dirk Jamieson
 */
public class UserService {
    private PluginConfig config;
    private UserRepository users;
    private BanRepository bans;
    private MuteRepository mutes;

    public UserService(PluginConfig config, UserRepository users, BanRepository bans, MuteRepository mutes) {
        this.config = config;
        this.users = users;
        this.bans = bans;
        this.mutes = mutes;
    }
    
    public User get(String name) {
        return users.findByName(name);
    }
    
    public User get(UUID id) {
        return users.find(id);
    }
    
    public User create(UUID id, String name, Instant lastPlayed) {
        User user = new User(id, name);
        user.setLastActive(lastPlayed);
        users.save(user);

        return user;
    }
    
    public User create(Player player) {
        return create(player.getUniqueId(), player.getName(), Instant.ofEpochMilli(player.getLastPlayed()));
    }
    
    public User get(Player player) {
        if(config.isOffline()) {
            return get(player.getName());
        } else {
            return get(player.getUniqueId());
        }
    }
    
    public User getOrCreate(Player player) {
        User user = get(player);
        
        if(user != null) return user;
        
        return create(player);
    }
    
    public boolean isBanned(User user) {
        return RestrictionUtil.isActive(user.getBan());
    }
    
    public boolean isMuted(User user) {
        return RestrictionUtil.isActive(user.getMute());
    }
    
    public void onJoin(User user) throws RejectedException {
        if(isBanned(user)) {
            Ban ban = user.getBan();

            throw new RejectedException("ban.denied")
                    .with("reason", ban.getReason())
                    .with("duration", ban.getExpiresAt());
        }
    }
    
    public void onChat(User user) throws RejectedException {
        if(isMuted(user)) {
            Mute mute = user.getMute();

            throw new RejectedException("mute.denied")
                    .with("reason", mute.getReason())
                    .with("duration", mute.getExpiresAt());
        }
    }

    public void onCommand(User user, String command) throws RejectedException {
        if(config.getChatCommands().contains(command.toLowerCase()) && isMuted(user)) {
            Mute mute = user.getMute();

            throw new RejectedException("mute.denied")
                    .with("reason", mute.getReason())
                    .with("duration", mute.getExpiresAt());
        }
    }

    public void ban(User source, User user, String reason, Duration duration) throws RejectedException {
        Ban ban = new Ban();
        ban.setCreated(Instant.now());
        ban.setReason(reason);
        ban.setSource(source);
        
        if(duration != null) {
            ban.setExpiresAt((Instant) duration.addTo(ban.getCreated()));
        }

        RestrictionUtil.assertRestrictionLonger(user.getBan(), ban);
        bans.save(ban);
        user.setBan(ban);
        users.save(user);
    }
    
    public void mute(User source, User user, String reason, Duration duration) throws RejectedException {
        Mute mute = new Mute();
        mute.setCreated(Instant.now());
        mute.setReason(reason);
        mute.setSource(source);
        
        if(duration != null) {
            mute.setExpiresAt(((Instant) duration.addTo(mute.getCreated())));
        }
        
        RestrictionUtil.assertRestrictionLonger(user.getMute(), mute);
        
        mutes.save(mute);

        user.setMute(mute);
        users.save(user);
    }
}

