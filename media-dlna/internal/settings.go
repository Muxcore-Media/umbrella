package internal

import (
	"fmt"
	"strings"

	"github.com/Muxcore-Media/core/pkg/contracts"
)

func (m *Module) Settings() []contracts.SettingDef {
	return m.settingsDefs()
}

func (m *Module) UpdateSetting(key, value string) error {
	return m.updateSetting(key, value)
}

func (m *Module) settingsDefs() []contracts.SettingDef {
	m.mu.RLock()
	friendly := m.friendlyName
	root := m.browseRoot
	paths := strings.Join(m.mediaPaths, ",")
	dlnaHTTP := m.dlnaHTTPAddr
	ifName := m.ifName
	noTranscode := m.noTranscode
	m.mu.RUnlock()
	return []contracts.SettingDef{
		{
			Key:         "friendly_name",
			Label:       "Friendly Name",
			Type:        contracts.SettingTypeString,
			Value:       friendly,
			Description: "UPnP device name shown to DLNA clients (DLNA_FRIENDLY_NAME)",
			Required:    true,
			Group:       "Server",
		},
		{
			Key:         "media_paths",
			Label:       "Media Paths",
			Type:        contracts.SettingTypeString,
			Value:       paths,
			Description: "Comma-separated library directories (DLNA_MEDIA_PATHS)",
			Required:    true,
			Group:       "Library",
		},
		{
			Key:         "browse_root",
			Label:       "Browse Root",
			Type:        contracts.SettingTypeString,
			Value:       root,
			Description: "Directory exposed to DLNA clients (derived from media paths)",
			Required:    true,
			Group:       "Library",
		},
		{
			Key:         "dlna_http_addr",
			Label:       "DLNA HTTP Address",
			Type:        contracts.SettingTypeString,
			Value:       dlnaHTTP,
			Description: "Bind address for UPnP/DLNA HTTP (DLNA_HTTP_ADDR)",
			Required:    false,
			Group:       "Network",
		},
		{
			Key:         "ifname",
			Label:       "Network Interface",
			Type:        contracts.SettingTypeString,
			Value:       ifName,
			Description: "Optional interface for SSDP announcements (DLNA_IFNAME)",
			Required:    false,
			Group:       "Network",
		},
		{
			Key:         "no_transcode",
			Label:       "Disable Transcoding",
			Type:        contracts.SettingTypeBool,
			Value:       fmt.Sprintf("%t", noTranscode),
			Description: "Serve files directly without ffmpeg transcodes (DLNA_NO_TRANSCODE)",
			Required:    false,
			Group:       "Playback",
		},
	}
}

func (m *Module) updateSetting(key, value string) error {
	switch key {
	case "friendly_name", "DLNA_FRIENDLY_NAME":
		m.mu.Lock()
		m.friendlyName = strings.TrimSpace(value)
		m.mu.Unlock()
	case "media_paths", "DLNA_MEDIA_PATHS", "DLNA_MEDIA_PATH":
		paths, err := resolveMediaPaths(splitPaths(value))
		if err != nil {
			return err
		}
		root, err := browseRootForPaths(paths)
		if err != nil {
			return err
		}
		m.mu.Lock()
		m.mediaPaths = paths
		m.browseRoot = root
		m.mu.Unlock()
	case "browse_root":
		value = strings.TrimSpace(value)
		if value == "" {
			return fmt.Errorf("browse_root must not be empty")
		}
		paths, err := resolveMediaPaths([]string{value})
		if err != nil {
			return err
		}
		m.mu.Lock()
		m.browseRoot = paths[0]
		m.mu.Unlock()
	case "dlna_http_addr", "DLNA_HTTP_ADDR":
		m.mu.Lock()
		m.dlnaHTTPAddr = strings.TrimSpace(value)
		m.mu.Unlock()
	case "ifname", "DLNA_IFNAME":
		m.mu.Lock()
		m.ifName = strings.TrimSpace(value)
		m.mu.Unlock()
	case "no_transcode", "DLNA_NO_TRANSCODE":
		m.mu.Lock()
		m.noTranscode = strings.EqualFold(strings.TrimSpace(value), "true") || value == "1"
		m.mu.Unlock()
	default:
		return fmt.Errorf("unknown setting %q", key)
	}
	return nil
}
