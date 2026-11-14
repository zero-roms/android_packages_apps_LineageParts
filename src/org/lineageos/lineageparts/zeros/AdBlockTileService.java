/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.zeros;

import android.app.AlertDialog;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.ContextThemeWrapper;

import org.lineageos.lineageparts.R;

/**
 * ZeroS ad and tracker blocking tile. The blocklist itself is applied by init
 * (vendor/zeros/adblock/zeros-adblock.rc): it is laid over /system/etc/hosts at
 * every start unless persist.sys.zeros.adblock is "false", and removed at once
 * when that property becomes "false". Switching it back on therefore needs a
 * restart; sys.zeros.adblock_pending (cleared by the restart) remembers that
 * one is due, so the tile can say so.
 */
public class AdBlockTileService extends TileService {

    private static final String PROP_SWITCH = "persist.sys.zeros.adblock";
    private static final String PROP_PENDING = "sys.zeros.adblock_pending";

    private static boolean isSwitchedOn() {
        return !"false".equals(SystemProperties.get(PROP_SWITCH, "true"));
    }

    private static boolean isRestartPending() {
        return "1".equals(SystemProperties.get(PROP_PENDING, ""));
    }

    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        if (isSwitchedOn()) {
            SystemProperties.set(PROP_SWITCH, "false");
            SystemProperties.set(PROP_PENDING, "0");
            update();
            return;
        }
        SystemProperties.set(PROP_SWITCH, "true");
        SystemProperties.set(PROP_PENDING, "1");
        update();
        Context themed = new ContextThemeWrapper(this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert);
        showDialog(new AlertDialog.Builder(themed)
                .setTitle(R.string.zeros_adblock_restart_title)
                .setMessage(R.string.zeros_adblock_restart_message)
                .setPositiveButton(R.string.zeros_adblock_restart_now, (dialog, which) ->
                        getSystemService(PowerManager.class).reboot(null))
                .setNegativeButton(R.string.zeros_adblock_restart_later, null)
                .create());
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean on = isSwitchedOn();
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(getString(!on ? R.string.zeros_adblock_off
                : isRestartPending() ? R.string.zeros_adblock_on_after_restart
                : R.string.zeros_adblock_on));
        tile.updateTile();
    }
}
