package playbackv1

import "testing"

func TestSessionFilterScopes(t *testing.T) {
	f := &SessionFilter{TmdbId: 584}
	if !f.HasMediaScope() {
		t.Fatal("expected media scope")
	}
	if f.HasIdentityScope() {
		t.Fatal("unexpected identity scope")
	}
	f = &SessionFilter{IdentityId: "uid-1", SinceUnix: 1}
	if !f.HasIdentityScope() || !f.HasTimeScope() {
		t.Fatal("expected identity and time scope")
	}
}

func TestStatsRangeQueryDefaults(t *testing.T) {
	var q *StatsRangeQuery
	if got := q.DaysOrDefault(30); got != 30 {
		t.Fatalf("got %d", got)
	}
	q = &StatsRangeQuery{Days: 7}
	if got := q.DaysOrDefault(30); got != 7 {
		t.Fatalf("got %d", got)
	}
}
