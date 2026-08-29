package playbackv1

import "strings"

// HasIdentityScope is true when the filter targets a specific user or identity.
func (f *SessionFilter) HasIdentityScope() bool {
	if f == nil {
		return false
	}
	return strings.TrimSpace(f.GetUserId()) != "" ||
		strings.TrimSpace(f.GetIdentityId()) != "" ||
		strings.TrimSpace(f.GetUserName()) != ""
}

// HasMediaScope is true when the filter targets specific media identifiers.
func (f *SessionFilter) HasMediaScope() bool {
	if f == nil {
		return false
	}
	return strings.TrimSpace(f.GetMediaId()) != "" ||
		strings.TrimSpace(f.GetRatingKey()) != "" ||
		strings.TrimSpace(f.GetMediaType()) != "" ||
		strings.TrimSpace(f.GetImdbId()) != "" ||
		f.GetTmdbId() != 0 ||
		f.GetTvdbId() != 0
}

// HasTimeScope is true when since/until bounds are set.
func (f *SessionFilter) HasTimeScope() bool {
	if f == nil {
		return false
	}
	return f.GetSinceUnix() != 0 || f.GetUntilUnix() != 0
}

// DaysOrDefault returns days when positive, otherwise defaultDays.
func (q *StatsRangeQuery) DaysOrDefault(defaultDays int32) int32 {
	if q == nil || q.GetDays() <= 0 {
		return defaultDays
	}
	return q.GetDays()
}

// ChartDaysOrDefault returns chart range days with fallback.
func (q *ChartQuery) ChartDaysOrDefault(defaultDays int32) int32 {
	if q == nil || q.GetRange() == nil {
		return defaultDays
	}
	return q.GetRange().DaysOrDefault(defaultDays)
}
