package testing.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.legacy.*;
import testing.ui.*;
import testing.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static testing.ui.TUDialogs.*;

public class BlockDialog extends BaseDialog{
    TextField search;
    Table all = new Table();
    Block block = Blocks.coreShard;
    Team placeTeam = Team.get(settings.getInt("tu-default-team", 1));
    int placePos, rotation = 1;
    static boolean initialized;

    boolean expectingPos;

    public BlockDialog(){
        super("@tu-block-menu.name");

        shouldPause = false;
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);


        all.margin(20).marginTop(0f);

        cont.table(s -> {
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        cont.pane(all);

        if(!initialized){
            Events.run(Trigger.update, () -> {
                if(expectingPos){
                    if(!state.isGame()){
                        expectingPos = false;
                    }else if(input.justTouched()){
                        if(!scene.hasMouse()){
                            int x = Math.round(input.mouseWorldX() / 8f), y = Math.round(input.mouseWorldY() / 8f);
                            placePos = Point2.pack(x, y);
                            ui.showInfoToast(bundle.format("tu-unit-menu.set-pos", x, y), 4f);
                            show();
                        }else{
                            ui.showInfoToast("@tu-unit-menu.cancel", 4f);
                        }
                        expectingPos = false;
                    }
                }
            });
            initialized = true;
        }
    }

    void rebuild(){
        expectingPos = false;
        all.clear();
        String text = search.getText();

        all.label(
            () -> bundle.get("tu-menu.selection") + block.localizedName
        ).padBottom(6);
        all.row();

        Seq<Block> array = content.blocks().select(b -> !b.isFloor() && !b.isStatic() && !(b instanceof ConstructBlock) && !(b instanceof LegacyBlock) && (text.isEmpty() || b.localizedName.toLowerCase().contains(text.toLowerCase())));
        all.table(list -> {
            list.left();

            float iconMul = 1.25f;
            int cols = (int)Mathf.clamp((graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 10) / iconMul, 1, 22 / iconMul);
            int count = 0;

            for(Block b : array){
                Image image = new Image(b.uiIcon).setScaling(Scaling.fit);
                list.add(image).size(8 * 4 * iconMul).pad(3);

                ClickListener listener = new ClickListener();
                image.addListener(listener);
                if(!mobile){
                    image.addListener(new HandCursorListener());
                    image.update(() -> image.color.lerp(listener.isOver() || block == b ? Color.white : Color.lightGray, Mathf.clamp(0.4f * Time.delta)));
                }else{
                    image.update(() -> image.color.lerp(block == b ? Color.white : Color.lightGray, Mathf.clamp(0.4f * Time.delta)));
                }

                image.clicked(() -> {
                    if(input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(b.name) != 0){
                        app.setClipboardText((char)Fonts.getUnicode(b.name) + "");
                        ui.showInfoFade("@copied");
                    }else{
                        block = b;
                    }
                });
                TUElements.boxTooltip(image, b.localizedName);

                if((++count) % cols == 0){
                    list.row();
                }
            }
        }).growX().left().padBottom(10);
        all.row();

        all.table(t -> {
            ImageButton tb = t.button(TUIcons.get(Icon.defense), TUStyles.lefti, TUVars.buttonSize, () -> teamDialog.show(placeTeam, team -> placeTeam = team)).get();
            tb.label(() -> bundle.format("tu-unit-menu.set-team", "[#" + placeTeam.color + "]" + teamName() + "[]")).padLeft(6).expandX();
            TUElements.boxTooltip(tb, "@tu-tooltip.block-set-team");

            ImageButton pb = t.button(TUIcons.get(Icon.map), TUStyles.toggleRighti, TUVars.buttonSize, () -> {
                hide();
                expectingPos = true;
            }).get();
            pb.label(() -> bundle.format("tu-unit-menu.pos", Point2.x(placePos), Point2.y(placePos))).padLeft(6).expandX();
            TUElements.boxTooltip(pb, "@tu-tooltip.block-pos");
        }).padTop(6);
        all.row();

        all.table(p -> {
            ImageButton rb = p.button(TUIcons.get(Icon.up), TUStyles.lefti, TUVars.buttonSize, () -> {
                rotation = Mathf.mod(rotation - 1, 4);
                Log.info(rotation);
            }).get();
            TUElements.boxTooltip(rb, "@tu-tooltip.block-rotate");
            rb.setDisabled(() -> !block.rotate);
            rb.update(() -> {
                ((TextureRegionDrawable)(rb.getStyle().imageUp)).setRegion(getDirection());
            });

            ImageButton pb = p.button(new TextureRegionDrawable(block.uiIcon), TUStyles.centeri, TUVars.buttonSize, this::placeBlock).expandX().get();
            TUElements.boxTooltip(pb, "@tu-tooltip.block-place");
            pb.setDisabled(() -> Vars.world.tile(placePos) == null);
            pb.label(() -> "@tu-block-menu.place")
                .update(l -> l.setColor(pb.isDisabled() ? Color.lightGray : Color.white)).padLeft(6).expandX();
            pb.update(() -> {
                ((TextureRegionDrawable)(pb.getStyle().imageUp)).setRegion(block.uiIcon);
            });

            ImageButton cb = p.button(TUIcons.get(Icon.cancel), TUStyles.righti, TUVars.buttonSize, this::deleteBlock).expandX().get();
            TUElements.boxTooltip(cb, "@tu-tooltip.block-delete");
            cb.setDisabled(() -> Vars.world.tile(placePos) == null);
            cb.label(() -> "@tu-block-menu.delete")
                .update(l -> l.setColor(pb.isDisabled() ? Color.lightGray : Color.white)).padLeft(6).expandX();
        }).padTop(6f);
    }

    TextureRegion getDirection(){
        TextureRegionDrawable tex = switch(rotation){
            case 1 -> Icon.up;
            case 2 -> Icon.left;
            case 3 -> Icon.down;
            default -> Icon.right;
        };
        return tex.getRegion();
    }

    void placeBlock(){
        if(Utils.noCheat()){
            if(net.client()){
                Utils.runCommand("Vars.world.tile(" + placePos + ").setNet(Vars.content.block(" + block.name + "),Team.get(" + placeTeam.id + ")," + rotation + ")");
            }else{
                world.tile(placePos).setNet(block, placeTeam, rotation);
            }
        }
    }

    void deleteBlock(){
        if(Utils.noCheat()){
            if(net.client()){
                Utils.runCommand("Vars.world.tile(" + placePos + ").setAir()");
            }else{
                world.tile(placePos).setAir();
            }
        }
    }

    String teamName(){
        return teamDialog.teamName(placeTeam);
    }

    public Block getBlock(){
        return block;
    }
}
