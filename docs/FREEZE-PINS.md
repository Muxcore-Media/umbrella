# Umbrella safe-pin freeze

Freeze recorded **2026-10-05** (`2026-10-05T13:53:35Z` UTC). Every **active** submodule
gitlink points at the default-branch tip at freeze time. Rename twins
(`media-ui`, `media-jellyfin`, `muxcorectl`), the archived dumps (`cache-memory`,
`custom-scripts`) and the historical `_wave1` are intentionally not bumped (listed as
*frozen*). `core.wiki` has no API-visible branch tip; its pin is set from the local checkout.

| Path | Repository | SHA | Branch |
| ---- | ---------- | --- | ------ |
| `admin-ui` | Muxcore-Media/admin-ui | `05c513359b50854779f5d75a374468f37b06a20b` | `main` |
| `ai-filter` | Muxcore-Media/ai-filter | `00ad72e11f2c5e70207c9f9dd7a0ecd1bd78258d` | `main` |
| `ai-librarian` | Muxcore-Media/ai-librarian | `a745eed05c09ddccf7d150a470e79dd943e6ef58` | `main` |
| `ai-recommend` | Muxcore-Media/ai-recommend | `199690b0b60e013b28b4dc1304b0ec4a593d6133` | `main` |
| `ai-runtime` | Muxcore-Media/ai-runtime | `c3c3cadf418a40582e0d6264497aa9ed96c04b6a` | `main` |
| `ai-subtitles` | Muxcore-Media/ai-subtitles | `bb925d953397c905de5e6cb2344281929d62971b` | `main` |
| `ai-tickets` | Muxcore-Media/ai-tickets | `e90d879d8b893e859ed90ac62a476023c616ede1` | `main` |
| `api-rest` | Muxcore-Media/api-rest | `bc51f508e126e5230a1d8f7f920d97b25e4244e4` | `master` |
| `auth-local` | Muxcore-Media/auth-local | `d1ec0fa9dc64e8acffae26818c1dd515de4746f0` | `master` |
| `auth-oidc` | Muxcore-Media/auth-oidc | `6ef8b639b05d252d2660ac62adff15e7bb17edae` | `main` |
| `backup-local` | Muxcore-Media/backup-local | `3cae570ab34ae42ef06e2dfb2238b36a17508438` | `master` |
| `branding` | Muxcore-Media/branding | `485d1bd93b7820b2c1ad3900167fe89511d7d816` | `main` |
| `cache-local` | Muxcore-Media/cache-local | `4615b1c60d24eac53f7a1476041f423dcf573fba` | `master` |
| `cache-memory` | Muxcore-Media/cache-memory | `3dc81e428c12812a1d1970c35928c39e90e4edd2` | *(frozen twin — not bumped)* |
| `cache-redis` | Muxcore-Media/cache-redis | `3efd893236feb86670dee6e53633d39f0336e1de` | `master` |
| `call-policy-default` | Muxcore-Media/call-policy-default | `03b4ba76f8ac622ed26f8eef8f0661687f220a08` | `master` |
| `circuitbreaker-simple` | Muxcore-Media/circuitbreaker-simple | `fa25b118ae6e08ce8905547af33ad5ad5038a64b` | `main` |
| `config-watcher` | Muxcore-Media/config-watcher | `01f7981cfb333dbd92dce4002c8236013aba00bf` | `master` |
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
| `core` | Muxcore-Media/core | `b42b5c155397c7bcf2f15674c9428bd09d2e2ea9` | `master` |
| `core.wiki` | Muxcore-Media/core.wiki | `5d1facd1f31350bbdb8947b64007e95d6644cb24` | `master` (local checkout) |
| `custom-scripts` | Muxcore-Media/custom-scripts | `7b8e65c71f3401d35a9d93a17202a2e37f21af0f` | *(frozen twin — not bumped)* |
| `database-postgres` | Muxcore-Media/database-postgres | `4b5195c627ce191500881fdfbc886c4f92b55136` | `main` |
| `database-sqlite` | Muxcore-Media/database-sqlite | `2654cc58c38099ef990bd86090e2a8c3fd99abc9` | `master` |
| `data-redaction-pattern` | Muxcore-Media/data-redaction-pattern | `91a6c94169dfda6cb9096c4ff43306603eff3cb6` | `main` |
| `distributed-lock-sqlite` | Muxcore-Media/distributed-lock-sqlite | `26f73347ecbd98085d2d2b91d6e652d64ce25b11` | `main` |
| `downloader-debrid` | Muxcore-Media/downloader-debrid | `f4e00a18ad421dbeb1665b68ae1291a655849ef2` | `main` |
| `downloader-native-torrent` | Muxcore-Media/downloader-native-torrent | `23dfcbe2af32ec3b1d7bac885f3c5d3a2ac04afb` | `main` |
| `downloader-native-usenet` | Muxcore-Media/downloader-native-usenet | `565dfe07f2912dfcf9baf7b16e93bc272f8c19d2` | `main` |
| `downloader-qbittorrent` | Muxcore-Media/downloader-qbittorrent | `c6114fbef3cede8761bec4138be9b352973d0437` | `main` |
| `downloader-sabnzbd` | Muxcore-Media/downloader-sabnzbd | `654925f9508ae471927caf3709a05c8599fcc909` | `main` |
| `emby` | Muxcore-Media/emby | `4bd6cd434b6d0f2154a2efa55ad4f68954cf4353` | `main` |
| `encryption-aesgcm` | Muxcore-Media/encryption-aesgcm | `efe357fe910103e5605c9b4fd4098d5d409a21c6` | `master` |
| `executor-shell` | Muxcore-Media/executor-shell | `bd6d938a78a5c1bcc9c26ef942b464eec1607261` | `master` |
| `feature-flags-file` | Muxcore-Media/feature-flags-file | `d15c1a38f71b7c89ecfca7ff90f3025c23507d5c` | `master` |
| `health-monitor` | Muxcore-Media/health-monitor | `cce10bac45bac4c27128e1138d16654704376493` | `master` |
| `indexer-piratebay` | Muxcore-Media/indexer-piratebay | `f81e5c203056b8a4ac5801984a8a1106c76b9c59` | `main` |
| `indexer-torznab` | Muxcore-Media/indexer-torznab | `5011aa5345e441b41678e2e0bacbd50b7acb9655` | `main` |
| `input-validate-jsonschema` | Muxcore-Media/input-validate-jsonschema | `80daf2da6a21db5987071081b475c381a98992d5` | `main` |
| `jellyfin` | Muxcore-Media/jellyfin | `d60371f615bb71eae7aa9a2174eafe6944e3056b` | `main` |
| `logging-file` | Muxcore-Media/logging-file | `8ba73cd970c951380765bf2c6fc798b332bb1423` | `main` |
| `media-audiobooks` | Muxcore-Media/media-audiobooks | `c2e14c145a9baeb993ba84212b1a42dc3b42cdb1` | `main` |
| `media-automation` | Muxcore-Media/media-automation | `8d79e51e1f1c685b165e17625e1bc4e0f3ca35a3` | `master` |
| `media-books` | Muxcore-Media/media-books | `9d263952b18078b003b7cfbefc6b134e229efca7` | `main` |
| `media-comics` | Muxcore-Media/media-comics | `d422ea81851f9e0262c1fcd61cc220bcc51c9b43` | `main` |
| `media-custom-formats` | Muxcore-Media/media-custom-formats | `3f3cc1971b641e2a5154fe060302e10c17704e16` | `master` |
| `media-dlna` | Muxcore-Media/media-dlna | `c0f5bba949eba9dd9555c2bf2ac553df3a148a8d` | `main` |
| `media-ffprobe` | Muxcore-Media/media-ffprobe | `dbd3387ef78cc94b2ca09f66453e970eb2486bcb` | `master` |
| `media-graph` | Muxcore-Media/media-graph | `343eb6be6b6af4713246deada56af03de60b021c` | `main` |
| `media-intro-outro` | Muxcore-Media/media-intro-outro | `7444522394613e6c3b97968f1633080d94134e6e` | `main` |
| `media-jellyfin` | Muxcore-Media/jellyfin | `dda887720acedb988b72656e57f618a3de0a9f88` | *(frozen twin — not bumped)* |
| `media-library-maintainer` | Muxcore-Media/media-library-maintainer | `0014b034799ccc28e1d117c4c6096f0b675c8436` | `main` |
| `media-list-sync` | Muxcore-Media/media-list-sync | `d67dadbd215cca5e34ca1322b8ee7324718ec680` | `master` |
| `media-movies` | Muxcore-Media/media-movies | `13d085a785553c17189f43b770340a74ea6a0581` | `master` |
| `media-music` | Muxcore-Media/media-music | `b065566b0560f18ba1e4d68e30d6bcd3645bdfdd` | `main` |
| `media-rename` | Muxcore-Media/media-rename | `ecb767464994e9005a823f4161139e8a68f3b609` | `master` |
| `media-root-folders` | Muxcore-Media/media-root-folders | `8242ecb9300869a2729c9d378cb3be7d9d44cea0` | `main` |
| `media-scanner` | Muxcore-Media/media-scanner | `72d8ee211b6ff5b14c76880a46b2f3d65e9e9105` | `master` |
| `media-subtitles` | Muxcore-Media/media-subtitles | `b019d15b4ffa5c74d8d920d9390d6596a7c639b0` | `master` |
| `media-tagging` | Muxcore-Media/media-tagging | `4dff89724587b8c05369fcfe0d263c13f3434b5e` | `main` |
| `media-transcoder` | Muxcore-Media/media-transcoder | `c4789c4c6de224d40c394c4109ba83ebfa000326` | `master` |
| `media-transcoder-pool` | Muxcore-Media/media-transcoder-pool | `efc6937fb3b0134c1aa5d34368c3578517a6755a` | `main` |
| `media-tvos-app` | Muxcore-Media/muxcore-tvos | `a424ac65641da8d17d36937448dda0f2e8bd7fc5` | `main` |
| `media-tvshows` | Muxcore-Media/media-tvshows | `bd629462e75719a0915dd532a64089f744885084` | `master` |
| `media-ui` | Muxcore-Media/media-ui-app | `cc4c2d40e2b79eb2a540aeab0ccda7c5e10d1a0b` | *(frozen twin — not bumped)* |
| `media-ui-app` | Muxcore-Media/media-ui-app | `329fc2c5102f41ee4b3b12cc3e186218a615d3d3` | `main` |
| `metadata-musicbrainz` | Muxcore-Media/metadata-musicbrainz | `4e58696e417c492da70b6720f7f9a37266b3ef75` | `main` |
| `metadata-tmdb` | Muxcore-Media/metadata-tmdb | `14ffc30a2520339c7a85e8ce7b9d62ffe12ac3cf` | `main` |
| `metrics-prometheus` | Muxcore-Media/metrics-prometheus | `2de5bce7199dbb9c8741b777f0c192218343c1c7` | `master` |
| `muxcorectl` | Muxcore-Media/muxcorectl-cli | `6f73e87ea108a69ad2c8cbfcb916828d0e7030a7` | *(frozen twin — not bumped)* |
| `muxcorectl-cli` | Muxcore-Media/muxcorectl-cli | `668341790e6c2c31d038c1c3d1bbcb8947346691` | `main` |
| `muxcore-docs` | Muxcore-Media/muxcore-docs | `9cf13efb99e2252bf9413b607c584bba98187944` | `main` |
| `muxcore-installer` | Muxcore-Media/muxcore-installer | `abaf8c50a8f847e81e0003163826e8809d6e5bf2` | `main` |
| `muxcore-ios` | Muxcore-Media/muxcore-ios | `830d14d4c373341a10c22fbe143b457dbb8ea35e` | `main` |
| `muxcore-module-starter` | Muxcore-Media/muxcore-module-starter | `529e8b4fcd10473e3d58ebd3d4c2a031ed8e61c7` | `main` |
| `muxcore-operator` | Muxcore-Media/muxcore-operator | `009d05fe04b15029131f4026f68b6541b43b4df7` | `main` |
| `_mvp` | Muxcore-Media/mvp | `9dac2882f3fe8ed1ddb573187deda7c9c60e5679` | `main` |
| `notification-apprise` | Muxcore-Media/notification-apprise | `0c22b017e718a4d90a14d0ae8a0c931865227673` | `main` |
| `notification-default` | Muxcore-Media/notification-default | `fd3a0a5ead658fa5c139677a2628add750bc3391` | `master` |
| `playback-guard` | Muxcore-Media/playback-guard | `20f9257f1ad793c7b7115316ca3bc822bae08156` | `main` |
| `playback-monitor` | Muxcore-Media/playback-monitor | `cff8c29316dcca7549a4e5a5177d3822beb68c01` | `main` |
| `plex` | Muxcore-Media/plex | `095573ec05a693332fea8341a7d700d003f216ef` | `main` |
| `publish-policy-default` | Muxcore-Media/publish-policy-default | `19b058c144d8639ecd383ab31692390a8190ab1b` | `master` |
| `ratelimit-tokenbucket` | Muxcore-Media/ratelimit-tokenbucket | `70048c0e568a4511999bc6a3800c3162f2c43f92` | `master` |
| `request-media` | Muxcore-Media/request-media | `97f7d27a94403957bd2faf91d73c266952c6bade` | `master` |
| `scheduler-cron` | Muxcore-Media/scheduler-cron | `845fdee3878d44a30b2f9ecea5fee0022a957bfe` | `master` |
| `secrets-file` | Muxcore-Media/secrets-file | `cb02f75f6519d1fca2cd051d10ec143b882c9d58` | `master` |
| `secrets-vault` | Muxcore-Media/secrets-vault | `1da1a903c2c16fc5e7e96c29cecff9a5052c9de1` | `master` |
| `serialization-safe` | Muxcore-Media/serialization-safe | `be1775441fb67b2779445230df3027b8ee328627` | `main` |
| `spool` | Muxcore-Media/spool | `4cbaf3b681e368a255e3e649bcf51a1e327fd342` | `main` |
| `spool-resolver-http` | Muxcore-Media/spool-resolver-http | `2ac87fd9e7ad1a615dcd73b7edd0554aeb8a9c0c` | `main` |
| `storage-ceph` | Muxcore-Media/storage-ceph | `1349b3c627f5712587e7e1b0d72a3b663f600db3` | `main` |
| `storage-overlay` | Muxcore-Media/storage-overlay | `4e797fd0d68332b4d20c4597c2cf81da12f933db` | `main` |
| `storage-s3` | Muxcore-Media/storage-s3 | `ec888ad67a921657da9566de316f18cb2bf6d85d` | `main` |
| `tracing-otlp` | Muxcore-Media/tracing-otlp | `297322192bad86580d7f79180c7ccadf54f94581` | `master` |
| `userdata-local` | Muxcore-Media/userdata-local | `476b047e77a2d9f6a8922228fcff0b4f29e05a25` | `main` |
| `_wave1` | Muxcore-Media/wave1 | `754a8d7ccdf3f3f55f1149fa125416e31784b97a` | *(frozen twin — not bumped)* |
| `worker-pool-memory` | Muxcore-Media/worker-pool-memory | `7d5adf6f8df89244c2f5df4bf16a2269fd44aa68` | `master` |
| `workflow-tapestry` | Muxcore-Media/workflow-tapestry | `34728fd9b2f65e2ad81ad223a052aac4168a0829` | `master` |

## Summary

- Bumped: 86
- Unchanged (already at tip): 16
- Skipped (rename twins): 6
- Failed / kept prior pin: 1
