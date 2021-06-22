package tun

import (
	"net"
	"os"
	"strings"
	"sync"
	"syscall"

	"github.com/kr328/tun2socket"
)

type adapter struct {
	device   *os.File
	stack    tun2socket.Stack
	blocking []*net.IPNet
	dns      net.IP
	mtu      int
	once     sync.Once
	stop     func()
}

var lock sync.Mutex
var instance *adapter

func (a *adapter) close() {
	_ = a.stack.Close()
	_ = a.device.Close()
}

func Start(fd, mtu int, dns string, blocking string, stop func()) error {
	lock.Lock()
	defer lock.Unlock()

	if instance != nil {
		instance.close()
	}

	_ = syscall.SetNonblock(fd, true)

	device := os.NewFile(uintptr(fd), "/dev/tun")
	stack, err := tun2socket.NewStack(mtu)
	if err != nil {
		_ = device.Close()

		return err
	}

	dn := net.ParseIP(dns)

	var blk []*net.IPNet

	for _, b := range strings.Split(blocking, ";") {
		_, n, err := net.ParseCIDR(b)
		if err != nil {
			device.Close()

			return err
		}

		blk = append(blk, n)
	}

	instance = &adapter{
		device:   device,
		stack:    stack,
		blocking: blk,
		dns:      dn,
		mtu:      mtu,
		once:     sync.Once{},
		stop:     stop,
	}

	go instance.rx()
	go instance.tx()
	go instance.tcp()
	go instance.udp()

	return nil
}

func Stop() {
	lock.Lock()
	defer lock.Unlock()

	if instance != nil {
		instance.close()
	}

	instance = nil
}
