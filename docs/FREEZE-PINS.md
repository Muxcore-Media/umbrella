# Umbrella safe-pin freeze

Freeze recorded **2026-09-07** (`2026-09-07T04:47:09Z` UTC). Every **active** submodule
gitlink points at the default-branch tip at freeze time. Rename twins
(`media-ui`, `media-jellyfin`, `muxcorectl`) are intentionally excluded.

Tracked by [umbrella#152](https://github.com/Muxcore-Media/umbrella/issues/152).

Regenerate with `./scripts/bump-submodules-to-tip.sh` (uses GitHub default-branch tips).

| Path | Repository | SHA | Branch |
| ---- | ---------- | --- | ------ |
| `admin-ui` | Muxcore-Media/admin-ui | `49e346b1ed1f68ad31a8c09945bfb16d0869112f` | `main` |
| `api-rest` | Muxcore-Media/api-rest | `9d1c60b634cabcf69f1900fa3e2907fa138e2d48` | `master` |
| `auth-local` | Muxcore-Media/auth-local | `4ba691c46a0b42e201d832416975013b8bebc72e` | `master` |
| `auth-oidc` | Muxcore-Media/auth-oidc | `e7391d29b43a8f449de71d1ef3922f285c41e307` | `main` |
| `backup-local` | Muxcore-Media/backup-local | `be3976a145cdfd4f0adb3b4b2a45038ae5f2713c` | `master` |
| `branding` | Muxcore-Media/branding | `f6c2713cc395dc536cde615f2c55c7e8ff404c22` | `main` |
| `cache-local` | Muxcore-Media/cache-local | `c5397d7bbb67661a82074f459093c16de8c4d189` | `master` |
| `cache-memory` | Muxcore-Media/cache-memory | `3dc81e428c12812a1d1970c35928c39e90e4edd2` | `master` |
| `cache-redis` | Muxcore-Media/cache-redis | `878af89c34b9cb4d15f7094897375fd4acb371dc` | `master` |
| `call-policy-default` | Muxcore-Media/call-policy-default | `635896d67b7057d4578ce20935bb6668f6f8d670` | `master` |
| `circuitbreaker-simple` | Muxcore-Media/circuitbreaker-simple | `ef730342ea32b60498c1c6d41e1d543fd42cfe5e` | `main` |
| `config-watcher` | Muxcore-Media/config-watcher | `0990ba5fc84889dad45e27c98cc9d7c27dd0866d` | `master` |
| `contracts-automation` | Muxcore-Media/contracts-automation | `f4dfd4dfaac19ae33c731ba38e7a6d9894fde1cc` | `main` |
| `contracts-downloader` | Muxcore-Media/contracts-downloader | `e25b15eb0efaed06902494c40189758a9c7fd2de` | `main` |
| `contracts-indexer` | Muxcore-Media/contracts-indexer | `c5e156033387e7f2f299a3574c88bf30f476d6a4` | `main` |
| `contracts-media` | Muxcore-Media/contracts-media | `80dbbf6e31a2b3a872447dbe1090f5674ef38f64` | `main` |
| `contracts-media-admin` | Muxcore-Media/contracts-media-admin | `350de7622545b28d3fe0952e5a83d205f759a7c1` | `main` |
| `contracts-metadata` | Muxcore-Media/contracts-metadata | `7fb61b3daacf42c2b106c8853195ac27792b0215` | `main` |
| `contracts-notification` | Muxcore-Media/contracts-notification | `4d023a6f5ace8654f107764139d03311ab34bb12` | `main` |
| `contracts-playback` | Muxcore-Media/contracts-playback | `c7a35caa1040e18ee6882ab96c4e3e6e175504fb` | `main` |
| `contracts-reconciler` | Muxcore-Media/contracts-reconciler | `0316054bf996c7e13a4a86d6a775fbc0af7b9d61` | `main` |
| `contracts-scanner` | Muxcore-Media/contracts-scanner | `a0eca5aa014b88ac0bf50abd2411ec6540ed76fd` | `main` |
| `core` | Muxcore-Media/core | `ab36a63d48acc6fc53009c1a2b433a7409f6c0e3` | `master` |
| `core.wiki` | Muxcore-Media/core.wiki | `e778f6bfd432418598f7812191d1cc47fa0abe6e` | *(unchanged — empty or missing branch)* |
| `custom-scripts` | Muxcore-Media/custom-scripts | `7b8e65c71f3401d35a9d93a17202a2e37f21af0f` | `master` |
| `database-postgres` | Muxcore-Media/database-postgres | `7470cb4237af473ad5845c6f271557661e428fd5` | `main` |
| `database-sqlite` | Muxcore-Media/database-sqlite | `7909c00374ab3f1ac20c05ed5c624883cab546df` | `master` |
| `data-redaction-pattern` | Muxcore-Media/data-redaction-pattern | `7725cf406662cf41df4cf4cc6ba41cb16b42a2cd` | `main` |
| `distributed-lock-sqlite` | Muxcore-Media/distributed-lock-sqlite | `8f6a2c699814c8e93ab8c5f1db623063573a5d64` | `main` |
| `downloader-debrid` | Muxcore-Media/downloader-debrid | `f2b08656f2621dfd1bf2d5ea858f185e07bc2128` | `main` |
| `downloader-native-torrent` | Muxcore-Media/downloader-native-torrent | `4d6d7ae69847758dd100e172f22c7e748e1450bc` | `main` |
| `downloader-native-usenet` | Muxcore-Media/downloader-native-usenet | `750184d792a9b5ab9bf99d3c891bd41570419544` | `main` |
| `downloader-qbittorrent` | Muxcore-Media/downloader-qbittorrent | `5288f27e72d0130b9ebfe32f7158cdc5a7058f6c` | `main` |
| `downloader-sabnzbd` | Muxcore-Media/downloader-sabnzbd | `aa5eb97bd3d899ab32d256d6235a661175b607ab` | `main` |
| `emby` | Muxcore-Media/emby | `37234ddae71010398feafc796015d7f13faf4d9c` | `main` |
| `encryption-aesgcm` | Muxcore-Media/encryption-aesgcm | `47007125c33b55a1871292a1250f9d2d03b33d0f` | `master` |
| `executor-shell` | Muxcore-Media/executor-shell | `d3bb59e460de789182e6762fa6c478a3bf0dd429` | `master` |
| `feature-flags-file` | Muxcore-Media/feature-flags-file | `3a26d99a9b73216df126bba27063999a11b1d742` | `master` |
| `health-monitor` | Muxcore-Media/health-monitor | `ef22959725ba4ee38eac756cee3d91d849fea8d0` | `master` |
| `indexer-piratebay` | Muxcore-Media/indexer-piratebay | `cdc7baed81aaa7aeb88d7dc18aebd161f9633950` | `main` |
| `indexer-torznab` | Muxcore-Media/indexer-torznab | `bb8081656fa0f076a26116930052dfd08741a411` | `main` |
| `input-validate-jsonschema` | Muxcore-Media/input-validate-jsonschema | `6ce639fe61f8c28dde6e83246408d9005a84cbb9` | `main` |
| `jellyfin` | Muxcore-Media/jellyfin | `dda887720acedb988b72656e57f618a3de0a9f88` | `main` |
| `logging-file` | Muxcore-Media/logging-file | `3cfd6e02700f3abd48a6ffc654b46094de0fbced` | `main` |
| `media-audiobooks` | Muxcore-Media/media-audiobooks | `06dd34f3d2043dff96793b758b53415adc70da25` | `main` |
| `media-automation` | Muxcore-Media/media-automation | `a46d3e7eb8f2be9658c04e57bf9cece7f57f0887` | `master` |
| `media-books` | Muxcore-Media/media-books | `1c73d8e6d5ec6f63649d1380edb3f8e757782911` | `main` |
| `media-comics` | Muxcore-Media/media-comics | `669bb92d426fadd34b3c49c870fd46709fa2dd5e` | `main` |
| `media-custom-formats` | Muxcore-Media/media-custom-formats | `350fd96f7245e10b1e80ad81d7465c4f2b6c6bf8` | `master` |
| `media-dlna` | Muxcore-Media/media-dlna | `fb15e69c606230a5608d627630faa01b94641b1d` | `main` |
| `media-ffprobe` | Muxcore-Media/media-ffprobe | `2df57aeeca6f8a7618fb406a5bec23a5888f506d` | `master` |
| `media-graph` | Muxcore-Media/media-graph | `0b4afd503dced5b9ac100e91336965c2fef797ce` | `main` |
| `media-intro-outro` | Muxcore-Media/media-intro-outro | `c88a192c5c060e92f551a11f1efdd0527a48864b` | `main` |
| `media-jellyfin` | Muxcore-Media/jellyfin | `85be21c8e507a2cdf4c3551167e7f71708e19898` | *(frozen twin — not bumped)* |
| `media-library-maintainer` | Muxcore-Media/media-library-maintainer | `562db284d6235cdb6152052ff11a01afc09d9c81` | `main` |
| `media-list-sync` | Muxcore-Media/media-list-sync | `f8fc6c199de1ef760ab2181d3cab9930163ebedb` | `master` |
| `media-movies` | Muxcore-Media/media-movies | `a6f5f8feb76e3ed068c1abd1e643cdf80b1a020c` | `master` |
| `media-music` | Muxcore-Media/media-music | `3002f248e851042f7ae49a86997478d3870929be` | `main` |
| `media-rename` | Muxcore-Media/media-rename | `05cf24af9a5dfc47d2bda2a521ca7bd58ce55b37` | `master` |
| `media-root-folders` | Muxcore-Media/media-root-folders | `b95a3d38cf5db5e517c81c2be3fca8595d2a291b` | `main` |
| `media-scanner` | Muxcore-Media/media-scanner | `395668c7035497a1516443de706ce7a66d7dc991` | `master` |
| `media-subtitles` | Muxcore-Media/media-subtitles | `8c61e1330a5ca7ae49580a36e07ee01483a59f1e` | `master` |
| `media-tagging` | Muxcore-Media/media-tagging | `b6e386263f756ca3e7aee5320d62d62e10e3f597` | `main` |
| `media-transcoder` | Muxcore-Media/media-transcoder | `524eb7dea2fcf8708a6da914a6041b0be445d931` | `master` |
| `media-transcoder-pool` | Muxcore-Media/media-transcoder-pool | `305ff04b44c0d9a3baa72ce7f88f5eb27846dcb3` | `main` |
| `media-tvos-app` | Muxcore-Media/muxcore-tvos | `55c90940c5f28f71e1b48303a51ac69b7f3f61c0` | `main` |
| `media-tvshows` | Muxcore-Media/media-tvshows | `a925ed8312d3b11b963a143b008f9ae7f915880d` | `master` |
| `media-ui` | Muxcore-Media/media-ui-app | `c35a18665a5d925d4622d04000d0d0ad02c874f4` | *(frozen twin — not bumped)* |
| `media-ui-app` | Muxcore-Media/media-ui-app | `cc4c2d40e2b79eb2a540aeab0ccda7c5e10d1a0b` | `main` |
| `media-ui-tv` | Muxcore-Media/media-ui-tv | `c2c32ac17e43a5a987f737f69c7e7a6130bef28a` | *(unchanged — empty or missing branch)* |
| `metadata-musicbrainz` | Muxcore-Media/metadata-musicbrainz | `be9431d2591bb773e0bebda962a9377a86c2d511` | *(unchanged — empty or missing branch)* |
| `metadata-tmdb` | Muxcore-Media/metadata-tmdb | `f6e8c83937930ae756614e8e7301b5494dea867e` | `main` |
| `metrics-prometheus` | Muxcore-Media/metrics-prometheus | `51081c0c42c8dfaa98a1785a740b672ec0925a46` | `master` |
| `muxcore-android` | Muxcore-Media/muxcore-android | `164fca8994c490b895646a36d1c79096b4884937` | *(unchanged — empty or missing branch)* |
| `muxcorectl` | Muxcore-Media/muxcorectl-cli | `53c1620c63a73b37d3bc4bed7852af47c4366779` | *(frozen twin — not bumped)* |
| `muxcorectl-cli` | Muxcore-Media/muxcorectl-cli | `6f73e87ea108a69ad2c8cbfcb916828d0e7030a7` | `main` |
| `muxcore-docs` | Muxcore-Media/muxcore-docs | `47844c3ed7d05a60822617e3af7afef1da5a084b` | `main` |
| `muxcore-installer` | Muxcore-Media/muxcore-installer | `2619f853ba2a46904caae8a21a96afc5008ae538` | `main` |
| `muxcore-ios` | Muxcore-Media/muxcore-ios | `fb1d76c5f8b3c15a0cd2a6ad65ed8acec3f20b50` | `main` |
| `muxcore-module-starter` | Muxcore-Media/muxcore-module-starter | `bb4def343ad1d83ec056112e3f01596ebfb313d8` | `main` |
| `muxcore-operator` | Muxcore-Media/muxcore-operator | `32911fff0a8efaebc529857f89721f08a8dc26bf` | `main` |
| `_mvp` | Muxcore-Media/mvp | `8dbe0040b70e4d118a03e7fa541f41536a3a04d2` | `main` |
| `notification-apprise` | Muxcore-Media/notification-apprise | `cc57036eea3965d8a7575ea0ce7efc933b64f654` | `main` |
| `notification-default` | Muxcore-Media/notification-default | `2f719deba8be443eafbc8743d46045a12eaba234` | `master` |
| `playback-contract` | Muxcore-Media/playback-contract | `f1d051e41366a21d107f1adf3c88ea1647cd3c54` | *(unchanged — empty or missing branch)* |
| `playback-guard` | Muxcore-Media/playback-guard | `066b4082cff895280493db5958ede2dda16c415c` | `main` |
| `playback-monitor` | Muxcore-Media/playback-monitor | `01f9da9b56748223c684cf1d6627b8023b1decd9` | `main` |
| `plex` | Muxcore-Media/plex | `b4984d38559d51aaee8948ce7184b2c55eed21ae` | `main` |
| `publish-policy-default` | Muxcore-Media/publish-policy-default | `7a0d8ffac00eaf7272f28c8d70709c656041ebcc` | `master` |
| `ratelimit-tokenbucket` | Muxcore-Media/ratelimit-tokenbucket | `ae0cc88585e3bede4b9d992dea170ebef22aa507` | `master` |
| `request-media` | Muxcore-Media/request-media | `f6b78447952b0ae9f70aaa3ca1bb00cc4b0e9ec2` | `master` |
| `scheduler-cron` | Muxcore-Media/scheduler-cron | `5187c24a967617b42819e087c514648484c25de4` | `master` |
| `secrets-file` | Muxcore-Media/secrets-file | `64b3202aeed79365efd2dc7f3b293d56f5809a6a` | `master` |
| `secrets-vault` | Muxcore-Media/secrets-vault | `4c6751b36deacb2dc87c9a8ccd8e04ec47f88261` | `master` |
| `serialization-safe` | Muxcore-Media/serialization-safe | `c2422c5a327d7f085ceb783e6355f7c8a4eb9545` | `main` |
| `spool` | Muxcore-Media/spool | `23f555f4097022c7202334dc40f508b051ae6ce5` | `main` |
| `spool-resolver-http` | Muxcore-Media/spool-resolver-http | `a6a6a6120b4f1902002d385bb0cb9501947e93d2` | `main` |
| `storage-ceph` | Muxcore-Media/storage-ceph | `10ddb192a727dec64530992faa4f2c02affc52be` | `main` |
| `storage-overlay` | Muxcore-Media/storage-overlay | `61480400d91562bf70503f4ccdc596e3e2696ede` | `main` |
| `storage-s3` | Muxcore-Media/storage-s3 | `d14217d112ec462b747c503e325aa643afc81c37` | `main` |
| `tracing-otlp` | Muxcore-Media/tracing-otlp | `4cdd83bcd14da0ae93d3711c2d8308b4dbf05bcd` | `master` |
| `userdata-local` | Muxcore-Media/userdata-local | `9e7582826854661eabd39e2e050c014f55ea8143` | `main` |
| `_wave1` | Muxcore-Media/wave1 | `754a8d7ccdf3f3f55f1149fa125416e31784b97a` | `main` |
| `worker-pool-memory` | Muxcore-Media/worker-pool-memory | `d9c2ad1cf48ac8fd1b9a69e0c3a0d285fa6834e3` | `master` |
| `workflow-tapestry` | Muxcore-Media/workflow-tapestry | `0d47cfa8d6a8e225ff5ab2e515ffaa8bd5a7f467` | `master` |

## Summary

- Bumped: 86
- Unchanged (already at tip): 11
- Skipped (rename twins): 3
- Failed / kept prior pin: 5

### Prior pins kept (empty GitHub mirror)

These paths already had valid gitlinks but their GitHub repos have no commits on the default branch yet; pins were left unchanged:

- `core.wiki`
- `media-ui-tv`
- `metadata-musicbrainz`
- `muxcore-android`
- `playback-contract`
