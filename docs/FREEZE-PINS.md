# Umbrella safe-pin freeze

Freeze recorded **2026-10-05** (`2026-10-05T18:21:49Z` UTC). Every **active** submodule
gitlink points at the default-branch tip at freeze time. Rename twins
(`media-ui`, `media-jellyfin`, `muxcorectl`), the archived dumps (`cache-memory`,
`custom-scripts`) and the historical `_wave1` are intentionally not bumped (listed as
*frozen*). `core.wiki` has no API-visible branch tip; its pin is set from the local checkout.

| Path | Repository | SHA | Branch |
| ---- | ---------- | --- | ------ |
| `admin-ui` | Muxcore-Media/admin-ui | `62e959bc5830433cfee18098b7b019e4dd6ca5cb` | `main` |
| `ai-filter` | Muxcore-Media/ai-filter | `a788a9e77098c9a03d21c826a457dc38ac56f45e` | `main` |
| `ai-librarian` | Muxcore-Media/ai-librarian | `35275177e3f0dc9221ea5539ccefa4c9b2e1682e` | `main` |
| `ai-recommend` | Muxcore-Media/ai-recommend | `b580c3983249ca9e22238fbc9a11bbd22e0c7605` | `main` |
| `ai-runtime` | Muxcore-Media/ai-runtime | `44574fb1daa08c02af366a54c4368cf863cf130a` | `main` |
| `ai-subtitles` | Muxcore-Media/ai-subtitles | `8889866f05a1d46b8e30da7067e349ce7620f589` | `main` |
| `ai-tickets` | Muxcore-Media/ai-tickets | `5995e862ed93182e1b2cb04d0cc1eaac5d679dc2` | `main` |
| `api-rest` | Muxcore-Media/api-rest | `2c3a7a5491b8b4dbfd39ce2d959ab5f5e9d93ddc` | `master` |
| `auth-local` | Muxcore-Media/auth-local | `7d2a6e79042c03993ae7f2c6d5f2e70b9fd11ab1` | `master` |
| `auth-oidc` | Muxcore-Media/auth-oidc | `636780bbde7a5d53dce7ee26b9c113bbc2dc63c0` | `main` |
| `backup-local` | Muxcore-Media/backup-local | `68e6f87ae0c08fe7bb9a51683c89e3bba61ae8ab` | `master` |
| `branding` | Muxcore-Media/branding | `485d1bd93b7820b2c1ad3900167fe89511d7d816` | `main` |
| `cache-local` | Muxcore-Media/cache-local | `a9c4a85741fb2e8ed9b960421c513eddd37e450e` | `master` |
| `cache-memory` | Muxcore-Media/cache-memory | `3dc81e428c12812a1d1970c35928c39e90e4edd2` | *(frozen twin — not bumped)* |
| `cache-redis` | Muxcore-Media/cache-redis | `b61dc93fe738eaeb0ea06b1d90a97f19148cfdd4` | `master` |
| `call-policy-default` | Muxcore-Media/call-policy-default | `356a0212f47fdcc269c6d54d4ba51939dfea3d54` | `master` |
| `circuitbreaker-simple` | Muxcore-Media/circuitbreaker-simple | `684e22012473ac9ed704549a9e4d1ce9e9e9f9e9` | `main` |
| `config-watcher` | Muxcore-Media/config-watcher | `9ebf3d6c4400a149668676f0a658aa5804cdf94e` | `master` |
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
| `core` | Muxcore-Media/core | `b9437bbd4fa2951cb1b5d608f66677cb02109284` | `master` |
| `core.wiki` | Muxcore-Media/core.wiki | `5d1facd1f31350bbdb8947b64007e95d6644cb24` | `master` (local checkout) |
| `custom-scripts` | Muxcore-Media/custom-scripts | `7b8e65c71f3401d35a9d93a17202a2e37f21af0f` | *(frozen twin — not bumped)* |
| `database-postgres` | Muxcore-Media/database-postgres | `e219e73037b75c9d6bbccfe7eb6afeec85f149f4` | `main` |
| `database-sqlite` | Muxcore-Media/database-sqlite | `3bcc0ae438a8f6b2c2f560f68a525090ae94603f` | `master` |
| `data-redaction-pattern` | Muxcore-Media/data-redaction-pattern | `99a0133d7ee01348286c1bd598c66356582c08ae` | `main` |
| `distributed-lock-sqlite` | Muxcore-Media/distributed-lock-sqlite | `32f2399c5070af480e291e4ee67059b49fb47491` | `main` |
| `downloader-debrid` | Muxcore-Media/downloader-debrid | `06b89c3a1ad75d6d963212b79c51530c0803e2b9` | `main` |
| `downloader-native-torrent` | Muxcore-Media/downloader-native-torrent | `b6adf9e79581b7ac4bc4ac94e0bff4171cc2ede4` | `main` |
| `downloader-native-usenet` | Muxcore-Media/downloader-native-usenet | `0b8addb3f8f8e56feb851b5458b102a621b6e2d4` | `main` |
| `downloader-qbittorrent` | Muxcore-Media/downloader-qbittorrent | `c7f0be1ec56fa54565398a15fd09f69a1fdac930` | `main` |
| `downloader-sabnzbd` | Muxcore-Media/downloader-sabnzbd | `6b881c410a01fc8347955c08d5d1c069a0d146c0` | `main` |
| `emby` | Muxcore-Media/emby | `22bf362ca601185e40280403d867f5748d3a89bb` | `main` |
| `encryption-aesgcm` | Muxcore-Media/encryption-aesgcm | `b31a3930871b4c0b773be09166764f6df07a388b` | `master` |
| `executor-shell` | Muxcore-Media/executor-shell | `799337766edfbe811c8f056eb127904cfdab4555` | `master` |
| `feature-flags-file` | Muxcore-Media/feature-flags-file | `391adc06957c2635391b1ef82bc44806d27d438d` | `master` |
| `health-monitor` | Muxcore-Media/health-monitor | `8651af5df75809019706f7c057a2f3a730f2fe8c` | `master` |
| `indexer-piratebay` | Muxcore-Media/indexer-piratebay | `3c6daf79eaf5bb99d6df6edcfd455533e0c845c1` | `main` |
| `indexer-torznab` | Muxcore-Media/indexer-torznab | `c601d63f99c4d04c420b12cb1ac4a06db36552fc` | `main` |
| `input-validate-jsonschema` | Muxcore-Media/input-validate-jsonschema | `d8ee76f48f222a7de44e385d2ab6872820dfe6d9` | `main` |
| `jellyfin` | Muxcore-Media/jellyfin | `e0dd36febb1fd3cc1a3a227a0c64f224153a4066` | `main` |
| `logging-file` | Muxcore-Media/logging-file | `4cfb9b0d68ae353476697decf54144aad1860405` | `main` |
| `media-audiobooks` | Muxcore-Media/media-audiobooks | `519bb91cd67ffddf0dedc1b4ed43883568166e83` | `main` |
| `media-automation` | Muxcore-Media/media-automation | `fcb298498f1667cc5b399e8f7f06e35f87218140` | `master` |
| `media-books` | Muxcore-Media/media-books | `576a32d8d9387d8bac250b20fe444cd37fb40f1f` | `main` |
| `media-comics` | Muxcore-Media/media-comics | `635bb07a43738a7230f952be2085b5ed6a0322d5` | `main` |
| `media-custom-formats` | Muxcore-Media/media-custom-formats | `d5fb85c1b3ed633007a2f95e6774a49837c59d23` | `master` |
| `media-dlna` | Muxcore-Media/media-dlna | `aa5d7b18e82b90945a43dc8ae1b9d6c95f00e469` | `main` |
| `media-ffprobe` | Muxcore-Media/media-ffprobe | `fea4cecf852b1f240b5e8b619c73e3e7801b58e1` | `master` |
| `media-graph` | Muxcore-Media/media-graph | `108466e3eb54ea8a56043b7553f4961fac2d10eb` | `main` |
| `media-intro-outro` | Muxcore-Media/media-intro-outro | `f1da2535a239869c215f3080f8f7e6a1d226f8d2` | `main` |
| `media-jellyfin` | Muxcore-Media/jellyfin | `dda887720acedb988b72656e57f618a3de0a9f88` | *(frozen twin — not bumped)* |
| `media-library-maintainer` | Muxcore-Media/media-library-maintainer | `c0d3ecb18d58e1c452edc17cea1f9f4bd6891cfa` | `main` |
| `media-list-sync` | Muxcore-Media/media-list-sync | `719c62bfaf22dfc6f6d15bd12a7f867611ead71f` | `master` |
| `media-movies` | Muxcore-Media/media-movies | `5db42ba6fd1ac29b0d49ce4c364100a6d448cebf` | `master` |
| `media-music` | Muxcore-Media/media-music | `e2a8ce37ef954a0c1234e4e7336a4d6951dbf6d4` | `main` |
| `media-rename` | Muxcore-Media/media-rename | `730d6f31fec8da35733f9191b5ffbabb4a6f9a41` | `master` |
| `media-root-folders` | Muxcore-Media/media-root-folders | `9078c07d4c7eb36fcb0dfd30cc7e92aaff4f39b1` | `main` |
| `media-scanner` | Muxcore-Media/media-scanner | `5595292e804c1284e0f0837cb797aecafa65ab64` | `master` |
| `media-subtitles` | Muxcore-Media/media-subtitles | `e652b60302d56b0dc034cc13efa0c9cb8e900d85` | `master` |
| `media-tagging` | Muxcore-Media/media-tagging | `a4a65eb69391ff3fef8ed9eb8111ced0fb3590cb` | `main` |
| `media-transcoder` | Muxcore-Media/media-transcoder | `eab357c0dc5fe0ac30823757e774d19a95ff4bc4` | `master` |
| `media-transcoder-pool` | Muxcore-Media/media-transcoder-pool | `8380852cf60ed5adfce73314a8f7fc52da937067` | `main` |
| `media-tvos-app` | Muxcore-Media/muxcore-tvos | `a424ac65641da8d17d36937448dda0f2e8bd7fc5` | `main` |
| `media-tvshows` | Muxcore-Media/media-tvshows | `94790a80f5d829c37571b52a143b0b6be28e2924` | `master` |
| `media-ui` | Muxcore-Media/media-ui-app | `cc4c2d40e2b79eb2a540aeab0ccda7c5e10d1a0b` | *(frozen twin — not bumped)* |
| `media-ui-app` | Muxcore-Media/media-ui-app | `329fc2c5102f41ee4b3b12cc3e186218a615d3d3` | `main` |
| `metadata-musicbrainz` | Muxcore-Media/metadata-musicbrainz | `6601739fd06bbbb4f1ae8462cbe5e58081c4f134` | `main` |
| `metadata-tmdb` | Muxcore-Media/metadata-tmdb | `bf5cf7aee67338eb475e57c07cda91c0e99d528d` | `main` |
| `metrics-prometheus` | Muxcore-Media/metrics-prometheus | `17d78e3c28f2fbdcb92116d98437ac0bb58fe005` | `master` |
| `muxcorectl` | Muxcore-Media/muxcorectl-cli | `6f73e87ea108a69ad2c8cbfcb916828d0e7030a7` | *(frozen twin — not bumped)* |
| `muxcorectl-cli` | Muxcore-Media/muxcorectl-cli | `a7f9cf97e3cbbd40f5fd76c0f33fe9a41c83c52f` | `main` |
| `muxcore-docs` | Muxcore-Media/muxcore-docs | `9cf13efb99e2252bf9413b607c584bba98187944` | `main` |
| `muxcore-installer` | Muxcore-Media/muxcore-installer | `283de6d89c51976af01626e14ab0cb6a4b9309cf` | `main` |
| `muxcore-ios` | Muxcore-Media/muxcore-ios | `830d14d4c373341a10c22fbe143b457dbb8ea35e` | `main` |
| `muxcore-module-starter` | Muxcore-Media/muxcore-module-starter | `db26dca96fa5cb87c3bcb53d3e0133c32bb77bb9` | `main` |
| `muxcore-operator` | Muxcore-Media/muxcore-operator | `64c6f8c36667e0a3aa3d8b14f6be29bb6265f386` | `main` |
| `_mvp` | Muxcore-Media/mvp | `9b3ae4a31e30f71e0fe4ad04034b22690c662805` | `main` |
| `notification-apprise` | Muxcore-Media/notification-apprise | `3da75a23b1d129f04ef4a8073b758ed08459e3ea` | `main` |
| `notification-default` | Muxcore-Media/notification-default | `609452d07fb558f959f0f451270e8b83e5fffea0` | `master` |
| `playback-guard` | Muxcore-Media/playback-guard | `845a10a4d9825f2b409243932bcb332df46f2b4e` | `main` |
| `playback-monitor` | Muxcore-Media/playback-monitor | `bf14e0ca2b667ec4fe23ff9762ac39ca0d1704f9` | `main` |
| `plex` | Muxcore-Media/plex | `790e4eb4836e0f918cabde01191cf887b4acfd24` | `main` |
| `publish-policy-default` | Muxcore-Media/publish-policy-default | `6ab64a01cda01535c8355dd04fda7756c348c7bd` | `master` |
| `ratelimit-tokenbucket` | Muxcore-Media/ratelimit-tokenbucket | `d4f833bab3e083f8b6e7618702ff6d10f5d70ead` | `master` |
| `request-media` | Muxcore-Media/request-media | `10483aaba5de6c3e6508020e0c218cb31d66be77` | `master` |
| `scheduler-cron` | Muxcore-Media/scheduler-cron | `6078357458a5e19229268a1d1f003fc9322eab40` | `master` |
| `secrets-file` | Muxcore-Media/secrets-file | `87a18ec337b60c51761981ad095bc5a5d03795ba` | `master` |
| `secrets-vault` | Muxcore-Media/secrets-vault | `8ad34bf427e2f22b9a92767e429c84e0e0c0a859` | `master` |
| `serialization-safe` | Muxcore-Media/serialization-safe | `56389acdca6adc9bec2c1adf7bc1177d35865d22` | `main` |
| `spool` | Muxcore-Media/spool | `202672288be622a6272bac9495d89fdf1fe0024d` | `main` |
| `spool-resolver-http` | Muxcore-Media/spool-resolver-http | `c68f58c489837d9e2d807ae8db71beebb56a7e73` | `main` |
| `storage-ceph` | Muxcore-Media/storage-ceph | `9880918d7da7c55c8d88f7b60a22d5972074ff68` | `main` |
| `storage-overlay` | Muxcore-Media/storage-overlay | `42d89175ebab747c8e352994b53c7124e27026ae` | `main` |
| `storage-s3` | Muxcore-Media/storage-s3 | `7c3d12e37e87f219044585f21cee8c1d95df4174` | `main` |
| `tracing-otlp` | Muxcore-Media/tracing-otlp | `cb81f24cdffebeca37587b3c11f3bc1e3982f581` | `master` |
| `userdata-local` | Muxcore-Media/userdata-local | `f8943fb3d1f03158511e3864a878c9beb7f56455` | `main` |
| `_wave1` | Muxcore-Media/wave1 | `754a8d7ccdf3f3f55f1149fa125416e31784b97a` | *(frozen twin — not bumped)* |
| `worker-pool-memory` | Muxcore-Media/worker-pool-memory | `0f6f8fbddaf0f95853d9ee55e27c3807127175f9` | `master` |
| `workflow-tapestry` | Muxcore-Media/workflow-tapestry | `5e1d25f3ae7aeb480945cb80d7dd69a1a4929992` | `master` |

## Summary

- Bumped: 17
- Unchanged (already at tip): 85
- Skipped (rename twins): 6
- Failed / kept prior pin: 1
