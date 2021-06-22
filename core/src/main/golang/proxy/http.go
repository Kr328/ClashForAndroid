package proxy

import (
	"sync"

	"github.com/Dreamacro/clash/listener/http"
	"github.com/Dreamacro/clash/tunnel"
)

var listener *http.Listener
var lock sync.Mutex

func Start(listen string) (listenAt string, err error) {
	lock.Lock()
	defer lock.Unlock()

	stopLocked()

	listener, err = http.NewWithAuthenticate(listen, tunnel.TCPIn(), false)
	if err == nil {
		listenAt = listener.Listener().Addr().String()
	}

	return
}

func Stop() {
	lock.Lock()
	defer lock.Unlock()

	stopLocked()
}

func stopLocked() {
	if listener != nil {
		listener.Close()
	}

	listener = nil
}
