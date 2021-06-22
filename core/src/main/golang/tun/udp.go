package tun

import (
	"net"

	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/transport/socks5"
	"github.com/kr328/tun2socket"

	"github.com/Dreamacro/clash/adapter/inbound"
	"github.com/Dreamacro/clash/common/pool"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
)

type packet struct {
	stack tun2socket.Stack
	local *net.UDPAddr
	data  []byte
}

func (pkt *packet) Data() []byte {
	return pkt.data
}

func (pkt *packet) WriteBack(b []byte, addr net.Addr) (n int, err error) {
	return pkt.stack.UDP().WriteTo(b, pkt.local, addr)
}

func (pkt *packet) Drop() {
	pool.Put(pkt.data)
}

func (pkt *packet) LocalAddr() net.Addr {
	return &net.UDPAddr{
		IP:   pkt.local.IP,
		Port: pkt.local.Port,
		Zone: "",
	}
}

func (a *adapter) udp() {
	log.Infoln("[ATUN] UDP receiver started")
	defer log.Infoln("[ATUN] UDP receiver exited")
	defer a.stack.Close()

read:
	for {
		buf := pool.Get(a.mtu)

		n, lAddr, rAddr, err := a.stack.UDP().ReadFrom(buf)
		if err != nil {
			return
		}

		sAddr := lAddr.(*net.UDPAddr)
		tAddr := rAddr.(*net.UDPAddr)

		// handle dns messages
		if a.hijackUDPDNS(buf[:n], sAddr, tAddr) {
			continue
		}

		// drop all packet send to blocking list
		for _, b := range a.blocking {
			if b.Contains(tAddr.IP) {
				continue read
			}
		}

		pkt := &packet{
			stack: a.stack,
			local: sAddr,
			data:  buf[:n],
		}

		tunnel.UDPIn() <- inbound.NewPacket(socks5.ParseAddrToSocksAddr(tAddr), pkt, C.SOCKS)
	}
}

func (a *adapter) hijackUDPDNS(pkt []byte, sAddr, tAddr *net.UDPAddr) bool {
	if !shouldHijackDns(a.dns, tAddr.IP, tAddr.Port) {
		return false
	}

	go func() {
		answer, err := relayDns(pkt)

		if err != nil {
			return
		}

		_, _ = a.stack.UDP().WriteTo(answer, sAddr, tAddr)

		pool.Put(pkt)
	}()

	return true
}
