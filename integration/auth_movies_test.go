//go:build integration

package integration

import (
	"testing"

	coreinteg "github.com/Muxcore-Media/core/integsupport"
)

func TestAuth_CoreMeshLifecycle(t *testing.T) {
	core := coreinteg.NewCoreHarness(t)

	// Core harness starts successfully — verify it has a valid address.
	if core.Addr == "" {
		t.Fatal("expected non-empty core address")
	}

	// Close should not panic.
	core.Close()
}
