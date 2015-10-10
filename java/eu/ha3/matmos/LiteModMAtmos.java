package eu.ha3.matmos;

import com.mumfrey.liteloader.HUDRenderListener;
import com.mumfrey.liteloader.Tickable;
import eu.ha3.matmos.serialize.dump.Dump;
import net.minecraft.client.Minecraft;

import java.io.File;

/**
 * @author dags_ <dags@dags.me>
 */

public class LiteModMAtmos implements Tickable, HUDRenderListener
{
    private final MAtmos mAtmos = new MAtmos();

    @Override
    public String getVersion()
    {
        return "2.0";
    }

    @Override
    public String getName()
    {
        return "MAtmos";
    }

    @Override
    public void init(File configPath)
    {
        mAtmos.init();
        new Dump(mAtmos.dataManager).dumpToJson(configPath);
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath)
    {

    }

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
    {
        if (inGame && clock)
        {
            mAtmos.onTick();
        }
    }

    @Override
    public void onPreRenderHUD(int screenWidth, int screenHeight)
    {
    }

    @Override
    public void onPostRenderHUD(int screenWidth, int screenHeight)
    {
        mAtmos.draw();
    }
}
