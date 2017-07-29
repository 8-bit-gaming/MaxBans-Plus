package org.maxgamer.maxbans.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maxgamer.maxbans.exception.PermissionException;
import org.maxgamer.maxbans.exception.RejectedException;
import org.maxgamer.maxbans.locale.Locale;
import org.maxgamer.maxbans.locale.MessageBuilder;
import org.maxgamer.maxbans.orm.Address;
import org.maxgamer.maxbans.orm.User;
import org.maxgamer.maxbans.service.*;
import org.maxgamer.maxbans.transaction.Transactor;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * @author netherfoam
 */
public class UnmuteCommandExecutor extends IPRestrictionCommandExecutor {
    private BroadcastService broadcastService;
    private AddressService addressService;
    private UserService userService;
    private MetricService metricService;

    public UnmuteCommandExecutor(Transactor transactor, Locale locale, Logger logger, LocatorService locatorService, BroadcastService broadcastService, AddressService addressService, UserService userService, MetricService metrics) {
        super(locale, logger, locatorService, "maxbans.mute", addressService, transactor);

        this.broadcastService = broadcastService;
        this.addressService = addressService;
        this.userService = userService;
        this.metricService = metrics;
    }

    @Override
    public void restrict(CommandSender sender, Address address, User user, Duration duration, String reason, boolean silent) throws RejectedException, PermissionException {
        User source = (sender instanceof Player ? userService.getOrCreate((Player) sender) : null);

        MessageBuilder message = locale.get()
                .with("source", source == null ? "Console" : source.getName());

        boolean any = false;
        if(user != null && userService.getMute(user) != null) {
            userService.unmute(source, user);
            message.with("name", user.getName());
            any = true;
        }

        if(addressService.getMute(address) != null) {
            addressService.unmute(source, address);
            message.with("address", address.getHost());
            any = true;
        }

        if(!any) {
            throw new RejectedException("No mute found");
        }

        broadcastService.broadcast(message.get("mute.unmute"), silent, sender);
    }
}
