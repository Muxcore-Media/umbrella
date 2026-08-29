package internal

import (
	"fmt"
	"strings"

	"github.com/Muxcore-Media/core/pkg/contracts"
)

func (m *Module) Settings() []contracts.SettingDef {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return []contracts.SettingDef{
		{
			Key:         "data_dir",
			Label:       "Data Directory",
			Type:        contracts.SettingTypeString,
			Value:       m.dataDir,
			Default:     "/var/lib/muxcore-userdata",
			Description: "Durable userdata JSON directory (USERDATA_LOCAL_DATA_DIR)",
			Group:       "Storage",
		},
	}
}

func (m *Module) UpdateSetting(key, value string) error {
	value = strings.TrimSpace(value)
	switch key {
	case "data_dir", "USERDATA_LOCAL_DATA_DIR":
		if value == "" {
			return fmt.Errorf("data_dir must not be empty")
		}
		m.mu.Lock()
		m.dataDir = value
		m.mu.Unlock()
		return nil
	default:
		return fmt.Errorf("unknown setting %q", key)
	}
}
