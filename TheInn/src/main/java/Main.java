import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

public class Main {

    private static final String legacy = "https://api.wynncraft.com/public_api.php?action=";

    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build().login().block();
        assert client != null;

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    final String content = event.getMessage().getContent();
                    if(content.startsWith("!track")) {
                        String guild = content.substring(7);

                        Objects.requireNonNull(event.getMessage().getChannel().block())
                                .createMessage("Tracking xp gained by members of " + guild)
                                .block();

                        guild = guild.replace(" ", "%20");

                        Hashtable<String, Long> xpCon = new Hashtable<>();
                        Hashtable<Integer, String> nTP = new Hashtable<>();
                        Hashtable<String, Long> pXPCon = new Hashtable<>();
                        Hashtable<Integer, String> pNTP = new Hashtable<>();
                        try {
                            while(true) {
                                URL players = new URL(legacy + "guildStats&command=" + guild);
                                HttpURLConnection con = (HttpURLConnection) players.openConnection();
                                con.setRequestMethod("GET");

                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(con.getInputStream())
                                );
                                String inputLine;
                                StringBuffer response = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                in.close();


                                String playerStats = response.toString().replaceAll("\"", "");


                                int count = 0;
                                String name = "";
                                Long contribution;
                                for (int i = 5; i < playerStats.length() - 12; i++) {
                                    if (playerStats.startsWith("name:", i))
                                        name = playerStats.substring(i + 5, playerStats.indexOf(",", i + 5));
                                    if (playerStats.substring(i, i + 12).equals("contributed:")) {
                                        contribution = Long.parseLong(playerStats.substring(i + 12, playerStats.indexOf(",", i + 12)));
                                        xpCon.put(name, contribution);
                                        nTP.put(count, name);
                                        count++;
                                    }
                                }

                                if(pXPCon.isEmpty()) {
                                    pXPCon.putAll(xpCon);
                                    pNTP.putAll(nTP);
                                }
                                int total = 0;
                                for (int i = 0; i < xpCon.size(); i++) {
                                    if(!pNTP.get(i).equals(nTP.get(i))) {
                                        if(nTP.size() > pNTP.size()) {
                                            pNTP.put(i, nTP.get(i));
                                            pXPCon.put(pNTP.get(i), 0L);
                                        }
                                        else {
                                            pXPCon.remove(pNTP.get(i));
                                            for(int k = i; k < nTP.size(); k++) {
                                                pNTP.replace(k, nTP.get(i));
                                            }
                                        }

                                    }
                                    long checkXP = xpCon.get(nTP.get(i)) - pXPCon.get(pNTP.get(i));
                                    total += checkXP;
                                    if(checkXP > 0) {
                                        Objects.requireNonNull(event.getMessage().getChannel().block())
                                                .createMessage(nTP.get(i) + "   -   " + checkXP + "/5m   -   " + checkXP/5 + "/1m")
                                                .block();

                                    }

                                }
                                if(total > 0)
                                    Objects.requireNonNull(event.getMessage().getChannel().block())
                                            .createMessage("**" + total + "xp grinded within the past 5 minutes!**")
                                            .block();
                                pXPCon.putAll(xpCon);
                                pNTP.putAll(nTP);
                                xpCon.clear();
                                nTP.clear();
                                Thread.sleep(300000);
                            }
                        } catch (Exception e) {
                            Objects.requireNonNull(event.getMessage().getChannel().block())
                                    .createMessage("Something went wrong").block();
                        }
                    }
                });

//t

        client.onDisconnect().block();
    }

    interface Command {
        void execute(MessageCreateEvent event);
    }
}

