package internal

import (
	"fmt"
	"os"

	"github.com/Muxcore-Media/core/pkg/contracts"
	"github.com/Muxcore-Media/downloader-native-usenet/internal/postproc"
)

func (m *Module) Settings() []contracts.SettingDef {
	cfg := nntpConfigFromEnv()
	pass := cfg.Pass
	if pass != "" {
		pass = "********"
	}
	return []contracts.SettingDef{
		{
			Key: "download_dir", Label: "Download directory", Type: contracts.SettingTypeString,
			Value: m.destDir, Description: "Root directory for completed usenet downloads", Group: "Paths",
		},
		{
			Key: "engine", Label: "Engine", Type: contracts.SettingTypeString,
			Value: m.engine, Description: "fixture (offline smoke) or live (NNTP)", Group: "Engine",
		},
		{
			Key: "par2_mode", Label: "PAR2 mode", Type: contracts.SettingTypeString,
			Value: string(postproc.ConfigFromEnv().PAR2), Description: "auto, skip, or require", Group: "Post-processing",
		},
		{
			Key: "unpack_mode", Label: "Unpack mode", Type: contracts.SettingTypeString,
			Value: string(postproc.ConfigFromEnv().Unpack), Description: "auto, skip, or require", Group: "Post-processing",
		},
		{
			Key: "cleanup", Label: "Delete archives after unpack", Type: contracts.SettingTypeBool,
			Value: fmt.Sprintf("%v", postproc.ConfigFromEnv().Cleanup), Group: "Post-processing",
		},
		{
			Key: "nntp_host", Label: "NNTP host", Type: contracts.SettingTypeString,
			Value: cfg.Host, Description: "Usenet provider hostname (required for live engine)", Group: "NNTP",
		},
		{
			Key: "nntp_port", Label: "NNTP port", Type: contracts.SettingTypeString,
			Value: fmt.Sprintf("%d", cfg.Port), Description: "Default 563 (TLS) or 119", Group: "NNTP",
		},
		{
			Key: "nntp_user", Label: "NNTP username", Type: contracts.SettingTypeString,
			Value: cfg.User, Group: "NNTP",
		},
		{
			Key: "nntp_pass", Label: "NNTP password", Type: contracts.SettingTypeSecret,
			Value: pass, Group: "NNTP",
		},
		{
			Key: "nntp_ssl", Label: "Use TLS", Type: contracts.SettingTypeBool,
			Value: fmt.Sprintf("%v", cfg.UseTLS), Group: "NNTP",
		},
	}
}

func (m *Module) UpdateSetting(key, value string) error {
	switch key {
	case "download_dir":
		if value == "" {
			return fmt.Errorf("download_dir required")
		}
		m.destDir = value
		m.jobs.destDir = value
	case "engine":
		if value != "fixture" && value != "live" {
			return fmt.Errorf("engine must be fixture or live")
		}
		m.engine = value
	case "par2_mode":
		_ = os.Setenv("USENET_PAR2", value)
	case "unpack_mode":
		_ = os.Setenv("USENET_UNPACK", value)
	case "cleanup":
		_ = os.Setenv("USENET_CLEANUP", value)
	case "nntp_host":
		_ = os.Setenv("NNTP_HOST", value)
	case "nntp_port":
		_ = os.Setenv("NNTP_PORT", value)
	case "nntp_user":
		_ = os.Setenv("NNTP_USER", value)
	case "nntp_pass":
		if value != "" && value != "********" {
			_ = os.Setenv("NNTP_PASS", value)
		}
	case "nntp_ssl":
		_ = os.Setenv("NNTP_SSL", value)
	default:
		return fmt.Errorf("unknown setting %q", key)
	}
	return nil
}
