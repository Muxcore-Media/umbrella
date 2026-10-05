# Umbrella safe-pin freeze

Freeze recorded **2026-10-05** (`2026-10-05T12:48:45Z` UTC). Every **active** submodule
gitlink points at the default-branch tip at freeze time. Rename twins
(`media-ui`, `media-jellyfin`, `muxcorectl`), the archived dumps (`cache-memory`,
`custom-scripts`) and the historical `_wave1` are intentionally not bumped (listed as
*frozen*). `core.wiki` has no API-visible branch tip; its pin is set from the local checkout.

| Path | Repository | SHA | Branch |
| ---- | ---------- | --- | ------ |
| `admin-ui` | Muxcore-Media/admin-ui | `5ac307cbd5a7a0bb80e5cdac44586d149e306293` | `main` |
| `ai-filter` | Muxcore-Media/ai-filter | `1337ca2a4044cd5b0f4684c79d0c092a199f036a` | `main` |
| `ai-librarian` | Muxcore-Media/ai-librarian | `0fc683e221930f0917dead4780091270e9a76720` | `main` |
| `ai-recommend` | Muxcore-Media/ai-recommend | `f19c732fe150c09f70a91388b4b1656470b9e7d9` | `main` |
| `ai-runtime` | Muxcore-Media/ai-runtime | `de26f65ee6c78a903344a144ff6bd6a7b2cb35d0` | `main` |
| `ai-subtitles` | Muxcore-Media/ai-subtitles | `769bd82d61953113a69db5b803ba409600ba56d8` | `main` |
| `ai-tickets` | Muxcore-Media/ai-tickets | `a725689666b96d8bb32cc811cdeb972788369336` | `main` |
| `api-rest` | Muxcore-Media/api-rest | `dd01a14b118baf32099a9834a4d3cb5cc3899038` | `master` |
| `auth-local` | Muxcore-Media/auth-local | `7e0d7e2d59f608ddda3544dcc198125cf69889ca` | `master` |
| `auth-oidc` | Muxcore-Media/auth-oidc | `2b542ca81fd84054e72eb583173f9c2acfbf748a` | `main` |
| `backup-local` | Muxcore-Media/backup-local | `ff755fbdfaeb443acd16593f1edf6cf991900622` | `master` |
| `branding` | Muxcore-Media/branding | `485d1bd93b7820b2c1ad3900167fe89511d7d816` | `main` |
| `cache-local` | Muxcore-Media/cache-local | `8fd572d70d4346c1a0182c9d71a70143e719766c` | `master` |
| `cache-memory` | Muxcore-Media/cache-memory | `3dc81e428c12812a1d1970c35928c39e90e4edd2` | *(frozen twin — not bumped)* |
| `cache-redis` | Muxcore-Media/cache-redis | `d5783876adc14dd0f164dabd12b101347bca2fd0` | `master` |
| `call-policy-default` | Muxcore-Media/call-policy-default | `effec9f16e9bfbe24f2ead775e2e24523632b957` | `master` |
| `circuitbreaker-simple` | Muxcore-Media/circuitbreaker-simple | `c6302bd3da4a413fec5f6a5c200dc652d7b90de9` | `main` |
| `config-watcher` | Muxcore-Media/config-watcher | `6e8d98792981dba31f125726ea585ec3af16a91e` | `master` |
| `contracts-ai` | Muxcore-Media/contracts-ai | `0f69a425a41498ece42c806e00faef9d81924f14` | `main` |
| `contracts-automation` | Muxcore-Media/contracts-automation | `9ce7f3c09d24974a7160c1598794361d8616bdf2` | `main` |
| `contracts-downloader` | Muxcore-Media/contracts-downloader | `d60820df46672952c481eeb794891799ef8a88a4` | `main` |
| `contracts-indexer` | Muxcore-Media/contracts-indexer | `933f811247abd680320513566db0e4e844e0eaed` | `main` |
| `contracts-media` | Muxcore-Media/contracts-media | `1bc51c3a58ae9e8ff5388b315f3285747ef53745` | `main` |
| `contracts-media-admin` | Muxcore-Media/contracts-media-admin | `bb846aafd0bf65d61ebc8f7ce8561070d4122651` | `main` |
| `contracts-metadata` | Muxcore-Media/contracts-metadata | `0e575f416cf50e4a05ea85d8f740023dea2415cf` | `main` |
| `contracts-notification` | Muxcore-Media/contracts-notification | `053927d14c756f19bb0a2322c280be1fa73ee41e` | `main` |
| `contracts-playback` | Muxcore-Media/contracts-playback | `af33cd4c9b9505aeb808c68fd0bd328e2160fab3` | `main` |
| `contracts-reconciler` | Muxcore-Media/contracts-reconciler | `d53d4bfaef08b69fea96f5433fb0959c2d5731af` | `main` |
| `contracts-scanner` | Muxcore-Media/contracts-scanner | `b92e5f7321026d0ff7515259c2f2bbc1c161d083` | `main` |
| `core` | Muxcore-Media/core | `bbc06b9ebc38723a2e6eeebfff28edd71e331710` | `master` |
| `core.wiki` | Muxcore-Media/core.wiki | `5d1facd1f31350bbdb8947b64007e95d6644cb24` | `master` (local checkout) |
| `custom-scripts` | Muxcore-Media/custom-scripts | `7b8e65c71f3401d35a9d93a17202a2e37f21af0f` | *(frozen twin — not bumped)* |
| `database-postgres` | Muxcore-Media/database-postgres | `f8d35af82c41dde248ea3e589f6342442785da79` | `main` |
| `database-sqlite` | Muxcore-Media/database-sqlite | `6f2f10a9a3311c5f2f51d487eae2ee41872767e3` | `master` |
| `data-redaction-pattern` | Muxcore-Media/data-redaction-pattern | `ab6f148a689af5a0ecd879ab1f5e788e651ba073` | `main` |
| `distributed-lock-sqlite` | Muxcore-Media/distributed-lock-sqlite | `44461e51b5dcd4305847c098f8536e742e3d0d0b` | `main` |
| `downloader-debrid` | Muxcore-Media/downloader-debrid | `64ca29c37b2452f8054455c572a078f39ea3ae72` | `main` |
| `downloader-native-torrent` | Muxcore-Media/downloader-native-torrent | `027082aa6e66b60c24d6f108f4d27700c8089f24` | `main` |
| `downloader-native-usenet` | Muxcore-Media/downloader-native-usenet | `a609a5782f0e60c90046fdfa3840a3a4e29563b1` | `main` |
| `downloader-qbittorrent` | Muxcore-Media/downloader-qbittorrent | `012556b0e2c239649766ed33dfdeb55030ce1ab2` | `main` |
| `downloader-sabnzbd` | Muxcore-Media/downloader-sabnzbd | `05247c5cf8985b587ac29ca7aea886ffcdbaf7b0` | `main` |
| `emby` | Muxcore-Media/emby | `e501b305112d0c68729e03dfa83d5860180b97d6` | `main` |
| `encryption-aesgcm` | Muxcore-Media/encryption-aesgcm | `aa5b87899386711cdf4c28548e081561595662fa` | `master` |
| `executor-shell` | Muxcore-Media/executor-shell | `649cd9a79422101337524f746caacd545aaa441c` | `master` |
| `feature-flags-file` | Muxcore-Media/feature-flags-file | `2f13d156df04de14c80695ba6b979e8a1eb528e2` | `master` |
| `health-monitor` | Muxcore-Media/health-monitor | `5df9c6d56f842821437bf879dd774dc196bdb722` | `master` |
| `indexer-piratebay` | Muxcore-Media/indexer-piratebay | `f4e07a655ce90334cf1e90c01b5c50d62f694532` | `main` |
| `indexer-torznab` | Muxcore-Media/indexer-torznab | `f4d2ed5c501fe0779ec44fd69bcc9bd0328deb5a` | `main` |
| `input-validate-jsonschema` | Muxcore-Media/input-validate-jsonschema | `731662f3e2c2972281944deaaee1ba6d8b9f8d4f` | `main` |
| `jellyfin` | Muxcore-Media/jellyfin | `bb24334bbb742fd2c0e03008d8002dbc12270aee` | `main` |
| `logging-file` | Muxcore-Media/logging-file | `6bae97b117be5b49ffd93e6614401fcfb277111a` | `main` |
| `media-audiobooks` | Muxcore-Media/media-audiobooks | `dd5b6f1db49daef6c4d44e530159aab38b9f4fa6` | `main` |
| `media-automation` | Muxcore-Media/media-automation | `b44b0e0b72f1362f1139d2e77d504d4df6b75478` | `master` |
| `media-books` | Muxcore-Media/media-books | `68f0d0f5dd8089d9f4edbb1428b1e3e608b6baa1` | `main` |
| `media-comics` | Muxcore-Media/media-comics | `337a5c7dee07f5cdaa6cbe076c5be4b2863e4b5a` | `main` |
| `media-custom-formats` | Muxcore-Media/media-custom-formats | `6cc021f351b31dbc59b036e952f8026465d2225f` | `master` |
| `media-dlna` | Muxcore-Media/media-dlna | `dbe32c8a4bfd950e9bfc322bd0b7ffd38ca25e20` | `main` |
| `media-ffprobe` | Muxcore-Media/media-ffprobe | `b90285e937fd1518f27ee9dee91f257fb36e034a` | `master` |
| `media-graph` | Muxcore-Media/media-graph | `b887f775924ef5d0d770b9cb6e32aba2fc2f95f3` | `main` |
| `media-intro-outro` | Muxcore-Media/media-intro-outro | `48dfa18f2fc6fbfeb67b06119850857ffffd73c1` | `main` |
| `media-jellyfin` | Muxcore-Media/jellyfin | `dda887720acedb988b72656e57f618a3de0a9f88` | *(frozen twin — not bumped)* |
| `media-library-maintainer` | Muxcore-Media/media-library-maintainer | `c4770761f67e7f32dbfd63773c7050be365078b0` | `main` |
| `media-list-sync` | Muxcore-Media/media-list-sync | `3e3ff446c5bf78933e4c827b746c0ae541e114b2` | `master` |
| `media-movies` | Muxcore-Media/media-movies | `0e5323861c626a259b74f1bc0604da1551913c7d` | `master` |
| `media-music` | Muxcore-Media/media-music | `e6c79095b7fbe9450856e993f2c79f7e8f85921e` | `main` |
| `media-rename` | Muxcore-Media/media-rename | `28e6591e7e74951223130ed22948ad62a9e1cb3e` | `master` |
| `media-root-folders` | Muxcore-Media/media-root-folders | `a1e48fd1311a64b7d98dcf9efa467b3c7cb0bf99` | `main` |
| `media-scanner` | Muxcore-Media/media-scanner | `9484b39da3813f79a4e873ad5b468561070152a1` | `master` |
| `media-subtitles` | Muxcore-Media/media-subtitles | `dfd934c777e17d1b6374bbb8f6fdb26c04fd2601` | `master` |
| `media-tagging` | Muxcore-Media/media-tagging | `b2cc34937305dc4f896dcea703c2c408220734af` | `main` |
| `media-transcoder` | Muxcore-Media/media-transcoder | `62963c29da83f9502de4e2980fc9f3001b195270` | `master` |
| `media-transcoder-pool` | Muxcore-Media/media-transcoder-pool | `aca1e57760c6a5b7c9f3b20ef6fb6875d2591711` | `main` |
| `media-tvos-app` | Muxcore-Media/muxcore-tvos | `a424ac65641da8d17d36937448dda0f2e8bd7fc5` | `main` |
| `media-tvshows` | Muxcore-Media/media-tvshows | `fd5b89374dfd6348c09a3966a847d28a192a4122` | `master` |
| `media-ui` | Muxcore-Media/media-ui-app | `cc4c2d40e2b79eb2a540aeab0ccda7c5e10d1a0b` | *(frozen twin — not bumped)* |
| `media-ui-app` | Muxcore-Media/media-ui-app | `329fc2c5102f41ee4b3b12cc3e186218a615d3d3` | `main` |
| `metadata-musicbrainz` | Muxcore-Media/metadata-musicbrainz | `8af43d6c1f1ea3ed877fb213bf6e906c1ef9a8b6` | `main` |
| `metadata-tmdb` | Muxcore-Media/metadata-tmdb | `82c971fe34b74202566cdae3b9a4087b323260d0` | `main` |
| `metrics-prometheus` | Muxcore-Media/metrics-prometheus | `0da65f5da8efec1ae23bd230a75525b124488872` | `master` |
| `muxcorectl` | Muxcore-Media/muxcorectl-cli | `6f73e87ea108a69ad2c8cbfcb916828d0e7030a7` | *(frozen twin — not bumped)* |
| `muxcorectl-cli` | Muxcore-Media/muxcorectl-cli | `3c5bacffcd3f131e8ed308422632655e66fa3173` | `main` |
| `muxcore-docs` | Muxcore-Media/muxcore-docs | `9cf13efb99e2252bf9413b607c584bba98187944` | `main` |
| `muxcore-installer` | Muxcore-Media/muxcore-installer | `8f77c8cdd86f2708bf23cfc6b2e49d93b593b68f` | `main` |
| `muxcore-ios` | Muxcore-Media/muxcore-ios | `830d14d4c373341a10c22fbe143b457dbb8ea35e` | `main` |
| `muxcore-module-starter` | Muxcore-Media/muxcore-module-starter | `0c1821a0de0264e83bd064ade3de887a2ebcab17` | `main` |
| `muxcore-operator` | Muxcore-Media/muxcore-operator | `2d354622abb405f951ac77451384b8f3808a570e` | `main` |
| `_mvp` | Muxcore-Media/mvp | `92810f5990085d53787de3a8bd8960276c9f71de` | `main` |
| `notification-apprise` | Muxcore-Media/notification-apprise | `7e5c0e894b361d5e7737341302f2dc52b72441cd` | `main` |
| `notification-default` | Muxcore-Media/notification-default | `e9e75a605ec01a3e283bf1d2418921b41536177a` | `master` |
| `playback-guard` | Muxcore-Media/playback-guard | `b0970f7214a0022f5f83595d8d426ab4100c0e63` | `main` |
| `playback-monitor` | Muxcore-Media/playback-monitor | `6a2287627217d1322d40709084a0224bcde3d41d` | `main` |
| `plex` | Muxcore-Media/plex | `1c0dac6a3d1c1281ed27ba8c2a10ec9e7f821a6d` | `main` |
| `publish-policy-default` | Muxcore-Media/publish-policy-default | `a20f27ac140f1935f6addf9de9c329d80fa90d93` | `master` |
| `ratelimit-tokenbucket` | Muxcore-Media/ratelimit-tokenbucket | `ac3ed5202e959294ee0ea4d65955f7aacffda873` | `master` |
| `request-media` | Muxcore-Media/request-media | `70211a1a6df69fa15397cdefaf99e722375a9898` | `master` |
| `scheduler-cron` | Muxcore-Media/scheduler-cron | `0b0bd7979b24d2f00688b9f767fe7cbae413146b` | `master` |
| `secrets-file` | Muxcore-Media/secrets-file | `ac79ad57076902b7b478ba8d4c98ec8610a276e2` | `master` |
| `secrets-vault` | Muxcore-Media/secrets-vault | `20da41354c0d70c047a864f14a6377154bf1f547` | `master` |
| `serialization-safe` | Muxcore-Media/serialization-safe | `45b04a472df8a66daa89bd84bc1ff1bdfb808f18` | `main` |
| `spool` | Muxcore-Media/spool | `7107796325cdca0e30c26fa92aaf5c0401d356be` | `main` |
| `spool-resolver-http` | Muxcore-Media/spool-resolver-http | `227ce920abad1e5af8291615e539510b7f703224` | `main` |
| `storage-ceph` | Muxcore-Media/storage-ceph | `a861fb09af2c0cbaef338d6c27b32dabe2df7889` | `main` |
| `storage-overlay` | Muxcore-Media/storage-overlay | `37b1281a53bd0b568eb2bd9d2d25d90a5a109fec` | `main` |
| `storage-s3` | Muxcore-Media/storage-s3 | `e0a8faf3db714c1de91131df13ff50cf76b46c74` | `main` |
| `tracing-otlp` | Muxcore-Media/tracing-otlp | `28eb9cbbd3abbcef55320495457af75f83f3d8dd` | `master` |
| `userdata-local` | Muxcore-Media/userdata-local | `248b8385e9a32e60fcef5c115655b2f627914c07` | `main` |
| `_wave1` | Muxcore-Media/wave1 | `754a8d7ccdf3f3f55f1149fa125416e31784b97a` | *(frozen twin — not bumped)* |
| `worker-pool-memory` | Muxcore-Media/worker-pool-memory | `2a4e2f3ea21beea1d447f321451dda8d001a70a4` | `master` |
| `workflow-tapestry` | Muxcore-Media/workflow-tapestry | `5020e06bac393fc31740014d6cf4b0c6aa5c030c` | `master` |

## Summary

- Bumped: 15
- Unchanged (already at tip): 87
- Skipped (rename twins): 6
- Failed / kept prior pin: 1
