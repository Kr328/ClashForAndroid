package tunnel

import "github.com/Dreamacro/clash/adapter/provider"

func Suspend(s bool) {
	provider.Suspend(s)
}
