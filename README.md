# 🎯 BowBoost
_A lightweight, packet-level bow boosting plugin for Minecraft 1.8.8_

**BowBoost** delivers crisp, configurable bow boosting using direct velocity packets.  
Shoot an arrow at yourself with a **Punch bow** to get an instant, smooth boost — no weird knockback, no rubber-banding.

---

## ✨ Features
- ⚡ **Instant & smooth boosts** via `PacketPlayOutEntityVelocity`
- ⚙️ **Fully configurable**: horizontal, vertical, and cobweb scaling
- 🕸 **Cobweb-aware**: optional horizontal multiplier when standing in webs
- 🔄 **Hot reload**: `/bowboost reload` updates `config.yml` and `messages.yml` on the fly
- 🛠️ **Lightweight & dependency-free**

---

## ⚙️ Configuration

**`config.yml`**
```yml
boost:
  horizontal: 1.2 # Base horizontal strength (scaled by Punch level, min 1.0)
  vertical: 0.42  # Upward force applied on boost
  web: 0.8        # Horizontal multiplier when inside cobwebs
messages.yml

yml
Copy code
no-permission: "&cYou don't have permission."
invalid-command: "&eUsage: /bowboost reload"
reload-success: "&aBowBoost configuration reloaded."
🕹 Commands & Permissions
Command: /bowboost reload

Permission: bowboost.command

📦 Installation
Drop the jar into your plugins/ folder.
Start the server to generate configs.
Edit config.yml and messages.yml as you like.
Run /bowboost reload to apply changes instantly.

⚠️ Important
KB THROUGH THE COBWEBS FOR THE ARROWS TO MAKE THEM SMOOTH DURING THE BOW BOOST OVER THE COBWEBS
“editable” – this allows you to block the block that is there when you try to bowboost over them.

⚙️ Anti-Rubberband Configuration (Spigot/Paper 1.8.8)
Tweak these server configs to reduce rubberbanding, rollbacks, and flying kicks when using custom boosts.

spigot.yml

yml
Copy code
settings:
  moved-wrongly-threshold: 0.5        # default 0.0625 is too strict for custom boosts
  moved-too-quickly-threshold: 2000.0 # raise a lot: prevents rubberband on high speeds
paper.yml / paper-spigot.yml (if you use PaperSpigot)

yml
Copy code
warnWhenSettingExcessiveVelocity: false # prevents spam and reduces conservative overrides
settings:
  limit-player-interactions: false      # optional; can reduce odd rollbacks
server.properties

properties
Copy code
allow-flight=true # avoids false "flying" positives when applying repeated vertical boosts (e.g. webs)
