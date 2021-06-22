package tun

import (
	"encoding/binary"
	"io"
	"net"
	"strconv"
	"time"

	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/context"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

const defaultDnsReadTimeout = time.Second * 30

func (a *adapter) tcp() {
	log.Infoln("[ATUN] TCP listener started")
	defer log.Infoln("[ATUN] TCP listener exited")
	defer a.stack.Close()

accept:
	for {
		conn, err := a.stack.TCP().Accept()
		if err != nil {
			return
		}

		sAddr := conn.LocalAddr().(*net.TCPAddr)
		tAddr := conn.RemoteAddr().(*net.TCPAddr)

		// handle dns messages
		if a.hijackTCPDNS(conn, tAddr) {
			continue
		}

		// drop all connections connect to blocking list
		for _, b := range a.blocking {
			if b.Contains(tAddr.IP) {
				_ = conn.Close()

				continue accept
			}
		}

		metadata := &C.Metadata{
			NetWork:    C.TCP,
			Type:       C.SOCKS,
			SrcIP:      sAddr.IP,
			DstIP:      tAddr.IP,
			SrcPort:    strconv.Itoa(sAddr.Port),
			DstPort:    strconv.Itoa(tAddr.Port),
			AddrType:   C.AtypIPv4,
			Host:       "",
			RawSrcAddr: sAddr,
			RawDstAddr: tAddr,
		}

		tunnel.TCPIn() <- context.NewConnContext(conn, metadata)
	}
}

func (a *adapter) hijackTCPDNS(conn net.Conn, tAddr *net.TCPAddr) bool {
	if !shouldHijackDns(a.dns, tAddr.IP, tAddr.Port) {
		return false
	}

	go func() {
		defer conn.Close()

		for {
			if err := conn.SetReadDeadline(time.Now().Add(defaultDnsReadTimeout)); err != nil {
				return
			}

			var length uint16
			if binary.Read(conn, binary.BigEndian, &length) != nil {
				return
			}

			data := make([]byte, length)

			_, err := io.ReadFull(conn, data)
			if err != nil {
				return
			}

			rb, err := relayDns(data)
			if err != nil {
				continue
			}

			if binary.Write(conn, binary.BigEndian, uint16(len(rb))) != nil {
				return
			}

			if _, err := conn.Write(rb); err != nil {
				return
			}
		}
	}()

	return true
}
