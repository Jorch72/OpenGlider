package gr8pefish.openglider.common.event;

import gr8pefish.openglider.api.capabilities.CapabilityHelper;
import gr8pefish.openglider.api.helper.GliderHelper;
import gr8pefish.openglider.common.capabilities.GliderCapabilityImplementation;
import gr8pefish.openglider.common.helper.OpenGliderPlayerHelper;
import gr8pefish.openglider.common.network.PacketHandler;
import gr8pefish.openglider.common.network.PacketUpdateClientTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ServerEventHandler {

    /**
     * Initialize the cap to the player.
     *
     * @param event - attach cap event
     */
    @SubscribeEvent
    public void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            if (!CapabilityHelper.hasGliderCapability((EntityPlayer)event.getObject())) {
                event.addCapability(GliderCapabilityImplementation.Provider.NAME, new GliderCapabilityImplementation.Provider());
            }
        }
    }

    /**
     * Deal with end movement and copying capability data over.
     *
     * @param event - the player being cloned (teleported in vanilla code)
     */
    @SubscribeEvent
    public void onPlayerCloning(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
//        if (!event.isWasDeath()) { //return from end (deal with dumb returning from the end code) //ToDo: Test without
            if (CapabilityHelper.hasGliderCapability(event.getOriginal())) {
                NBTTagCompound gliderData = CapabilityHelper.getGliderCapability(event.getOriginal()).serializeNBT();
                CapabilityHelper.getGliderCapability(event.getEntityPlayer()).deserializeNBT(gliderData);
            }
//        }
    }

    /**
     * Update the position of the player when flying.
     *
     * @param event - tick event
     */
    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event){
        if (GliderHelper.getIsGliderDeployed(event.player)){
            OpenGliderPlayerHelper.updatePosition(event.player);
        }
    }

    /**
     * Sync capability of a tracked player for visual person flying updates.
     *
     * @param event - the tracking event
     */
    @SubscribeEvent
    public void onTrack(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        EntityPlayer tracker = event.getEntityPlayer(); //the tracker
        Entity targetEntity = event.getTarget(); //the target that is being tracked
        if (targetEntity instanceof EntityPlayerMP) { //only entityPlayerMP ( MP part is very important!)
            EntityPlayer targetPlayer = (EntityPlayer) targetEntity; //typecast to entityPlayer
            if (CapabilityHelper.hasGliderCapability(targetPlayer)) { //if have the capability
                if (GliderHelper.getIsGliderDeployed(targetPlayer)) { //if the target has capability need to update
                    PacketHandler.HANDLER.sendTo(new PacketUpdateClientTarget(targetPlayer, true), (EntityPlayerMP) tracker); //send a packet to the tracker's client to update their target
                } else {
                    PacketHandler.HANDLER.sendTo(new PacketUpdateClientTarget(targetPlayer, false), (EntityPlayerMP) tracker);
                }
            }
        }
    }

    //===========================================================Simple Sync Capability EVents==============================================

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncGlidingCapability(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncGlidingCapability(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncGlidingCapability(event.player);
    }

    /**
     * Sends a message to the client to update the status of the glider to whatever it is on the server.
     *
     * @param player - the player to sync the data for
     */
    private void syncGlidingCapability(EntityPlayer player) {
        CapabilityHelper.getGliderCapability(player).sync((EntityPlayerMP)player);
    }

}
